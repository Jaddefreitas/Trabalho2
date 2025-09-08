package wepayu.service.exceptions;

public class EmployeeNotFoundException extends RuntimeException {
    public EmployeeNotFoundException(String id) {
        super("Empregado nao existe.");
    }
}
