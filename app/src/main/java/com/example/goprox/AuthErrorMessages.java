package com.example.goprox;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.FirebaseNetworkException;

final class AuthErrorMessages {
    private AuthErrorMessages() { }

    static String signIn(Exception error) {
        if (error instanceof FirebaseNetworkException) {
            return "Connection problem. Check your internet and try again.";
        }
        String code = error instanceof FirebaseAuthException
                ? ((FirebaseAuthException) error).getErrorCode() : "";
        switch (code) {
            case "ERROR_INVALID_EMAIL":
            case "INVALID_EMAIL":
                return "Enter a valid email address.";
            case "ERROR_USER_DISABLED":
            case "USER_DISABLED":
                return "This account is disabled. Contact support.";
            case "ERROR_TOO_MANY_REQUESTS":
            case "TOO_MANY_ATTEMPTS_TRY_LATER":
                return "Too many attempts. Wait a while and try again.";
            case "ERROR_NETWORK_REQUEST_FAILED":
            case "NETWORK_REQUEST_FAILED":
                return "Connection problem. Check your internet and try again.";
            case "ERROR_WRONG_PASSWORD":
            case "ERROR_USER_NOT_FOUND":
            case "ERROR_INVALID_CREDENTIAL":
            case "INVALID_LOGIN_CREDENTIALS":
                return "Email or password is incorrect.";
            default:
                return "Couldn't sign in. Please try again.";
        }
    }

    static String registration(Exception error) {
        if (error instanceof FirebaseNetworkException) {
            return "Connection problem. Check your internet and try again.";
        }
        String code = error instanceof FirebaseAuthException
                ? ((FirebaseAuthException) error).getErrorCode() : "";
        switch (code) {
            case "ERROR_WEAK_PASSWORD":
            case "WEAK_PASSWORD":
                return "Choose a stronger password with at least 6 characters.";
            case "ERROR_INVALID_EMAIL":
            case "INVALID_EMAIL":
                return "Enter a valid email address.";
            case "ERROR_TOO_MANY_REQUESTS":
            case "TOO_MANY_ATTEMPTS_TRY_LATER":
                return "Too many attempts. Wait a while and try again.";
            case "ERROR_NETWORK_REQUEST_FAILED":
            case "NETWORK_REQUEST_FAILED":
                return "Connection problem. Check your internet and try again.";
            case "ERROR_EMAIL_ALREADY_IN_USE":
            case "EMAIL_EXISTS":
                return "Couldn't create the account. Try signing in or resetting your password.";
            default:
                return "Couldn't create the account. Please try again.";
        }
    }
}
