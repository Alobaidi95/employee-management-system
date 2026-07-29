package com.example.EmployeeManagementSystem.exception;

public class InvalidTaskStateException extends RuntimeException{
    public InvalidTaskStateException(String message) {
        super(message);
    }
}
