package com.deepakkhatri.qa.data;

/**
 * A test account and why it exists. The description appears in test names and
 * the Allure report, so a failure says which behaviour was being exercised
 * rather than only which credentials were used.
 */
public record TestUser(String username, String password, String description) {

    @Override
    public String toString() {
        return description;
    }
}
