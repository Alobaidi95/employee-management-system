package com.example.EmployeeManagementSystem.exception;

public class UnMatchedPasswordsException extends RuntimeException {
    public UnMatchedPasswordsException(String message) {
        super(message);
    }
}
