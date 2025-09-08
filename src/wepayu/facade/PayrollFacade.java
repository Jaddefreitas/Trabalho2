package wepayu.facade;

import wepayu.model.*;
import wepayu.service.*;
import wepayu.service.exceptions.*;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class PayrollFacade {

    // formatador para salário/comissão no formato brasileiro (23,32)
    private static final DecimalFormat df = new DecimalFormat("#0.00",
            new DecimalFormatSymbols(new Locale("pt", "BR")));

    // Reinicia o sistema
    public void zerarSistema() {
        PayrollDatabase.clear();
        CommandManager.clear();
    }

    // Encerra o sistema
    public void encerrarSistema() {
        PayrollDatabase.clear();
    }

    // Criar empregados
    public String criarEmpregado(String nome, String endereco, String tipo, String salarioStr) {
        return criarEmpregado(nome, endereco, tipo, salarioStr, null);
    }

    public String criarEmpregado(String nome, String endereco, String tipo, String salarioStr, String comissaoStr) {
        if (nome == null || nome.isBlank()) throw new InvalidDataException("Nome nao pode ser nulo.");
        if (endereco == null || endereco.isBlank()) throw new InvalidDataException("Endereco nao pode ser nulo.");
        if (tipo == null || tipo.isBlank()) throw new InvalidDataException("Tipo invalido.");
        if (salarioStr == null || salarioStr.isBlank()) throw new InvalidDataException("Salario nao pode ser nulo.");

        double salario;
        try {
            salario = Double.parseDouble(salarioStr.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new InvalidDataException("Salario deve ser numerico.");
        }
        if (salario < 0) throw new InvalidDataException("Salario deve ser nao-negativo.");

        Employee e;
        switch (tipo.toLowerCase()) {
            case "horista":
                if (comissaoStr != null) throw new InvalidDataException("Tipo nao aplicavel.");
                e = new HourlyEmployee(nome, endereco, salario);
                break;
            case "assalariado":
                if (comissaoStr != null) throw new InvalidDataException("Tipo nao aplicavel.");
                e = new SalariedEmployee(nome, endereco, salario);
                break;
            case "comissionado":
                if (comissaoStr == null) {
                    throw new InvalidDataException("Tipo nao aplicavel.");
                }
                if (comissaoStr.isBlank()) {
                    throw new InvalidDataException("Comissao nao pode ser nula.");
                }
                double comissao;
                try {
                    comissao = Double.parseDouble(comissaoStr.replace(",", "."));
                } catch (NumberFormatException ex) {
                    throw new InvalidDataException("Comissao deve ser numerica.");
                }
                if (comissao < 0) throw new InvalidDataException("Comissao deve ser nao-negativa.");
                e = new CommissionedEmployee(nome, endereco, salario, comissao);
                break;
            default:
                throw new InvalidDataException("Tipo invalido.");
        }

        CommandManager.executeCommand(new AddEmployeeCommand(e));
        return e.getId();
    }

    // Remover empregado
    public void removerEmpregado(String id) {
        if (id == null || id.isBlank()) {
            throw new InvalidDataException("Identificacao do empregado nao pode ser nula.");
        }
        if (PayrollDatabase.getEmployee(id) == null) {
            throw new EmployeeNotFoundException("Empregado nao existe.");
        }
        CommandManager.executeCommand(new RemoveEmployeeCommand(id));
    }

    // Alterar atributos
    public void alteraEmpregado(String id, String atributo, String valor1) {
        if (id == null || atributo == null) {
            throw new InvalidDataException("Identificacao do empregado nao pode ser nula.");
        }
        Employee e = PayrollDatabase.getEmployee(id);
        if (e == null) {
            throw new EmployeeNotFoundException("Empregado nao existe.");
        }

        switch (atributo.toLowerCase()) {
            case "nome":
                e.setName(valor1);
                break;
            case "endereco":
                e.setAddress(valor1);
                break;
            case "metodopagamento":
                if (valor1 == null) throw new InvalidDataException("Metodo de pagamento invalido.");
                if (valor1.equalsIgnoreCase("banco")) {
                    e.setPaymentMethod(PaymentMethod.DEPOSITO_BANCARIO);
                } else if (valor1.equalsIgnoreCase("correios")) {
                    e.setPaymentMethod(PaymentMethod.CHEQUE_CORREIOS);
                } else if (valor1.equalsIgnoreCase("maos")) {
                    e.setPaymentMethod(PaymentMethod.CHEQUE_MAOS);
                } else {
                    throw new InvalidDataException("Metodo de pagamento invalido.");
                }
                break;
            case "sindicalizado":
                if (valor1 != null && valor1.equalsIgnoreCase("false")) {
                    e.setUnionMembership(null);
                }
                break;
            default:
                throw new InvalidDataException("Atributo nao existe.");
        }
    }

    public void alteraEmpregadoSindicato(String id, String unionId, double taxa) {
        Employee e = PayrollDatabase.getEmployee(id);
        if (e == null) throw new EmployeeNotFoundException("Empregado nao existe.");
        e.setUnionMembership(new UnionMembership(unionId, taxa));
    }

    // Lançamentos
    public void lancaCartao(String id, String data, double horas) {
        if (id == null || data == null) throw new InvalidDataException("Identificacao do empregado nao pode ser nula.");
        Employee e = PayrollDatabase.getEmployee(id);
        if (e == null) throw new EmployeeNotFoundException("Empregado nao existe.");
        if (!(e instanceof HourlyEmployee)) throw new InvalidDataException("Tipo nao aplicavel.");
        ((HourlyEmployee) e).addTimeCard(new TimeCard(data, horas));
    }

    public void lancaVenda(String id, String data, double valor) {
        if (id == null || data == null) throw new InvalidDataException("Identificacao do empregado nao pode ser nula.");
        Employee e = PayrollDatabase.getEmployee(id);
        if (e == null) throw new EmployeeNotFoundException("Empregado nao existe.");
        if (!(e instanceof CommissionedEmployee)) throw new InvalidDataException("Tipo nao aplicavel.");
        ((CommissionedEmployee) e).addSalesReceipt(new SalesReceipt(data, valor));
    }

    public void lancaTaxaServico(String id, String data, double valor) {
        if (id == null || data == null) throw new InvalidDataException("Identificacao do empregado nao pode ser nula.");
        Employee e = PayrollDatabase.getEmployee(id);
        if (e == null) throw new EmployeeNotFoundException("Empregado nao existe.");
        if (e.getUnionMembership() == null) throw new InvalidDataException("Empregado nao eh sindicalizado.");
        e.getUnionMembership().addServiceCharge(new ServiceCharge(data, valor));
    }

    // Obter atributos de empregado
    public String getAtributoEmpregado(String id, String atributo) {
        if (id == null || id.isBlank()) throw new InvalidDataException("Identificacao do empregado nao pode ser nula.");
        Employee e = PayrollDatabase.getEmployee(id);
        if (e == null) throw new EmployeeNotFoundException("Empregado nao existe.");

        switch (atributo.toLowerCase()) {
            case "nome": return e.getName();
            case "endereco": return e.getAddress();
            case "tipo":
                if (e instanceof HourlyEmployee) return "horista";
                if (e instanceof SalariedEmployee && !(e instanceof CommissionedEmployee)) return "assalariado";
                if (e instanceof CommissionedEmployee) return "comissionado";
                break;
            case "salario":
                if (e instanceof HourlyEmployee) return df.format(((HourlyEmployee) e).getHourlyRate());
                if (e instanceof SalariedEmployee && !(e instanceof CommissionedEmployee))
                    return df.format(((SalariedEmployee) e).getMonthlySalary());
                if (e instanceof CommissionedEmployee)
                    return df.format(((CommissionedEmployee) e).getMonthlySalary());
                break;
            case "comissao":
                if (e instanceof CommissionedEmployee) return df.format(((CommissionedEmployee) e).getCommissionRate());
                throw new InvalidDataException("Tipo nao aplicavel.");
            case "sindicalizado":
                return (e.getUnionMembership() != null) ? "true" : "false";
            default:
                throw new InvalidDataException("Atributo nao existe.");
        }
        throw new InvalidDataException("Atributo nao existe.");
    }

    // Rodar a folha
    public void rodaFolha(String data) {
        if (data == null) throw new InvalidDataException("Data nao pode ser nula.");
        List<Paycheck> checks = PayrollService.runPayroll(data);
        for (Paycheck pc : checks) {
            System.out.println("Contracheque: Empregado " + pc.getEmployeeId() +
                    " | Bruto: R$" + pc.getGrossPay() +
                    " | Deducoes: R$" + pc.getDeductions() +
                    " | Liquido: R$" + pc.getNetPay());
        }
    }

    // Undo/Redo
    public void undo() {
        CommandManager.undo();
    }

    public void redo() {
        CommandManager.redo();
    }
}
