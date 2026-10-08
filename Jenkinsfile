// Declarative pipeline for a Jenkins-based CI, kept alongside the GitHub
// Actions workflow because both appear in real engagements.
pipeline {
    agent any

    tools {
        jdk 'jdk-17'
        maven 'maven-3.9'
    }

    parameters {
        choice(name: 'SUITE', choices: ['testng-smoke.xml', 'testng.xml'],
               description: 'Which TestNG suite to run')
        choice(name: 'BROWSER', choices: ['chrome', 'firefox', 'edge'],
               description: 'Browser under test')
        booleanParam(name: 'USE_GRID', defaultValue: false,
                     description: 'Run against a Selenium Grid instead of local browsers')
        string(name: 'THREADS', defaultValue: '3',
               description: 'Parallel threads for TestNG')
    }

    options {
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timestamps()
    }

    triggers {
        // Nightly regression at 18:00.
        cron('0 18 * * *')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh './mvnw -B clean test-compile'
            }
        }

        stage('Start Grid') {
            when { expression { params.USE_GRID } }
            steps {
                sh 'docker compose -f docker-compose.grid.yml up -d'
                // Wait for the hub to report ready rather than sleeping.
                sh '''
                    for i in $(seq 1 30); do
                        if curl -sf http://localhost:4444/wd/hub/status | grep -q '"ready": *true'; then
                            echo "Grid ready"; exit 0
                        fi
                        sleep 2
                    done
                    echo "Grid did not become ready"; exit 1
                '''
            }
        }

        stage('Test') {
            steps {
                script {
                    def profile = params.USE_GRID ? '-Pgrid' : ''
                    sh """
                        ./mvnw -B test ${profile} \
                          -Dsuite=${params.SUITE} \
                          -Dbrowser=${params.BROWSER} \
                          -Dthreads=${params.THREADS}
                    """
                }
            }
        }
    }

    post {
        always {
            // Publish results even when the build failed — a failing run's
            // report is the one that needs reading.
            junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true

            script {
                if (fileExists('target/allure-results')) {
                    allure includeProperties: false,
                           jdk: '',
                           results: [[path: 'target/allure-results']]
                }
            }

            archiveArtifacts artifacts: 'target/surefire-reports/**',
                             allowEmptyArchive: true, fingerprint: true

            sh 'docker compose -f docker-compose.grid.yml down --remove-orphans || true'
        }
    }
}
