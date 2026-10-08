package com.deepakkhatri.qa.data;

import com.deepakkhatri.qa.config.Configuration;

/** Accounts published by the demo application, referenced by intent. */
public final class Users {

    private Users() {
        // Constant holder.
    }

    public static TestUser standard() {
        return new TestUser("standard_user", Configuration.password(),
                "happy-path user with no injected defects");
    }

    public static TestUser lockedOut() {
        return new TestUser("locked_out_user", Configuration.password(),
                "account disabled by the service");
    }

    public static TestUser problem() {
        return new TestUser("problem_user", Configuration.password(),
                "user with seeded UI defects");
    }

    /** Error copy the application returns for rejected sign-in attempts. */
    public static final class Errors {

        public static final String LOCKED_OUT =
                "Epic sadface: Sorry, this user has been locked out.";
        public static final String BAD_CREDENTIALS =
                "Epic sadface: Username and password do not match any user in this service";
        public static final String USERNAME_REQUIRED =
                "Epic sadface: Username is required";
        public static final String PASSWORD_REQUIRED =
                "Epic sadface: Password is required";

        private Errors() {
            // Constant holder.
        }
    }
}
