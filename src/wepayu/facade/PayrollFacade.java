package wepayu.facade;

import wepayu.model.*;
import wepayu.service.*;
import wepayu.service.exceptions.*;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class PayrollFacade {
    // Sobrecarga para aceitar valor como String (com vírgula)
    public void lancaTaxaServico(String id, String data, String valorStr) {
        if (id == null || id.isBlank() || data == null || valorStr == null)
            throw new InvalidDataException("Identificacao do membro nao pode ser nula.");
        if (!wepayu.util.DateUtils.isValidDate(data))
            throw new InvalidDataException("Data invalida.");
        double valor;
        try {
            valor = Double.parseDouble(valorStr.replace(",", "."));
        } catch (NumberFormatException ex) {
            throw new InvalidDataException("Valor deve ser numerico.");
        }
        if (valor <= 0) throw new InvalidDataException("Valor deve ser positivo.");
        lancaTaxaServico(id, data, valor);
    }
    /**
     * Retorna o total de taxas de serviço pagas por um empregado sindicalizado em um intervalo de datas.
     * @param id ID do empregado
     * @param dataInicial Data inicial (inclusive)
     * @param dataFinal Data final (exclusive)
     * @return Total de taxas de serviço (String, formato brasileiro)
     */
    public String getTaxasServico(String id, String dataInicial, String dataFinal) {
        if (id == null || id.isBlank()) throw new InvalidDataException("Identificacao do empregado nao pode ser nula.");
        if (!wepayu.util.DateUtils.isValidDate(dataInicial)) throw new InvalidDataException("Data inicial invalida.");
        if (!wepayu.util.DateUtils.isValidDate(dataFinal)) throw new InvalidDataException("Data final invalida.");
        if (wepayu.util.DateUtils.compareDates(dataInicial, dataFinal) > 0) throw new InvalidDataException("Data inicial nao pode ser posterior aa data final.");
        Employee e = PayrollDatabase.getEmployee(id);
        if (e == null) throw new EmployeeNotFoundException("Empregado nao existe.");
        if (e.getUnionMembership() == null) throw new InvalidDataException("Empregado nao eh sindicalizado.");
        double total = 0.0;
        for (ServiceCharge sc : e.getUnionMembership().getServiceCharges()) {
            if (wepayu.util.DateUtils.isBetweenExclusiveEnd(sc.getDate(), dataInicial, dataFinal)) {
                String valorStr = String.valueOf(sc.getAmount());
                double valor;
                try {
                    valor = Double.parseDouble(valorStr.replace(",", "."));
                } catch (NumberFormatException ex) {
                    valor = sc.getAmount();
                }
                total += valor;
            }
        }
        return df.format(total);
    }
    /**
     * Altera o status de sindicalização do empregado, incluindo id do sindicato e taxa sindical.
     * @param id ID do empregado
     * @param atributo deve ser "sindicalizado"
     * @param valor "true" para sindicalizar
     * @param idSindicato id do sindicato
     * @param taxaSindical taxa sindical (String, aceita vírgula)
     */
    public void alteraEmpregado(String id, String atributo, String valor, String idSindicato, String taxaSindical) {
        if (id == null || id.isBlank()) throw new InvalidDataException("Identificacao do empregado nao pode ser nula.");
        if (atributo == null || !"sindicalizado".equalsIgnoreCase(atributo)) throw new InvalidDataException("Atributo nao existe.");
        Employee e = PayrollDatabase.getEmployee(id);
        if (e == null) throw new EmployeeNotFoundException("Empregado nao existe.");
        if (valor != null && valor instanceof String && ((String)valor).equalsIgnoreCase("true")) {
            if (idSindicato == null || idSindicato.isBlank()) throw new InvalidDataException("Id do sindicato nao pode ser nulo.");
            if (taxaSindical == null || taxaSindical.isBlank()) throw new InvalidDataException("Taxa sindical nao pode ser nula.");
            double taxa;
            try {
                taxa = Double.parseDouble(taxaSindical.replace(",", "."));
            } catch (NumberFormatException ex) {
                throw new InvalidDataException("Taxa sindical deve ser numerica.");
            }
            if (taxa < 0) throw new InvalidDataException("Taxa sindical deve ser nao-negativa.");
            for (Employee emp : PayrollDatabase.getAllEmployees().values()) {
                if (emp.getUnionMembership() != null && idSindicato.equals(emp.getUnionMembership().getUnionId()) && !emp.getId().equals(id)) {
                    throw new InvalidDataException("Ha outro empregado com esta identificacao de sindicato");
                }
            }
            e.setUnionMembership(new UnionMembership(idSindicato, taxa));
        } else if (valor != null && valor instanceof String && ((String)valor).equalsIgnoreCase("false")) {
            e.setUnionMembership(null);
        } else {
            throw new InvalidDataException("Valor de sindicalizado deve ser true ou false.");
        }
    }
    /**
     * Retorna o total de vendas realizadas por um empregado comissionado em um intervalo de datas.
     * @param id ID do empregado
     * @param dataInicial Data inicial (inclusive)
     * @param dataFinal Data final (exclusive)
     * @return Total de vendas realizadas (String, formato brasileiro)
     */
    public String getVendasRealizadas(String id, String dataInicial, String dataFinal) {
        if (id == null || id.isBlank()) throw new InvalidDataException("Identificacao do empregado nao pode ser nula.");
        if (!wepayu.util.DateUtils.isValidDate(dataInicial)) throw new InvalidDataException("Data inicial invalida.");
        if (!wepayu.util.DateUtils.isValidDate(dataFinal)) throw new InvalidDataException("Data final invalida.");
        if (wepayu.util.DateUtils.compareDates(dataInicial, dataFinal) > 0) throw new InvalidDataException("Data inicial nao pode ser posterior aa data final.");
        Employee e = PayrollDatabase.getEmployee(id);
        if (e == null) throw new EmployeeNotFoundException("Empregado nao existe.");
        if (!(e instanceof CommissionedEmployee)) throw new InvalidDataException("Empregado nao eh comissionado.");
        double total = 0.0;
        for (SalesReceipt sr : ((CommissionedEmployee) e).sales) {
            if (wepayu.util.DateUtils.isBetweenExclusiveEnd(sr.getDate(), dataInicial, dataFinal)) {
                String valorStr = String.valueOf(sr.getAmount());
                double valor;
                try {
                    valor = Double.parseDouble(valorStr.replace(",", "."));
                } catch (NumberFormatException ex) {
                    valor = sr.getAmount();
                }
                total += valor;
            }
        }
        return df.format(total);
    }
    /**
     * Retorna o total de horas extras trabalhadas por um empregado horista em um intervalo de datas.
     * O valor retornado é inteiro, conforme esperado pelo teste.
     * @param id ID do empregado
     * @param dataInicial Data inicial (inclusive)
     * @param dataFinal Data final (exclusive)
     * @return Horas extras trabalhadas (int)
     */
    public String getHorasExtrasTrabalhadas(String id, String dataInicial, String dataFinal) {
        if (id == null || id.isBlank()) throw new InvalidDataException("Identificacao do empregado nao pode ser nula.");
        Employee e = PayrollDatabase.getEmployee(id);
        if (e == null) throw new EmployeeNotFoundException("Empregado nao existe.");
        if (!(e instanceof HourlyEmployee)) throw new InvalidDataException("Tipo nao aplicavel.");
        double horas = ((HourlyEmployee) e).getHorasTrabalhadas(dataInicial, dataFinal, true);
        // Formata com vírgula, sem casas decimais desnecessárias
        String horasStr = String.format(Locale.FRANCE, "%.1f", horas).replace('.', ',');
        // Remove vírgula zero (ex: 1,0 -> 1)
        if (horasStr.endsWith(",0")) horasStr = horasStr.substring(0, horasStr.length() - 2);
        return horasStr;
    }

    // formatador para salário/comissão no formato brasileiro (23,32)
    private static final DecimalFormat df = new DecimalFormat("#0.00",
            new DecimalFormatSymbols(Locale.forLanguageTag("pt-BR")));

        /**
         * Retorna o total de horas normais trabalhadas por um empregado horista em um intervalo de datas.
         * O valor retornado é inteiro, conforme esperado pelo teste.
         * @param id ID do empregado
         * @param dataInicial Data inicial (inclusive)
         * @param dataFinal Data final (exclusive)
         * @return Horas normais trabalhadas (int)
         */
        public int getHorasNormaisTrabalhadas(String id, String dataInicial, String dataFinal) {
            if (id == null || id.isBlank()) throw new InvalidDataException("Identificacao do empregado nao pode ser nula.");
            if (!wepayu.util.DateUtils.isValidDate(dataInicial)) throw new InvalidDataException("Data inicial invalida.");
            if (!wepayu.util.DateUtils.isValidDate(dataFinal)) throw new InvalidDataException("Data final invalida.");
            if (wepayu.util.DateUtils.compareDates(dataInicial, dataFinal) > 0) throw new InvalidDataException("Data inicial nao pode ser posterior aa data final.");
            Employee e = PayrollDatabase.getEmployee(id);
            if (e == null) throw new EmployeeNotFoundException("Empregado nao existe.");
            if (!(e instanceof HourlyEmployee)) throw new InvalidDataException("Empregado nao eh horista.");
            double horas = ((HourlyEmployee) e).getHorasTrabalhadas(dataInicial, dataFinal, false);
            return (int) horas;
        }
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
    public void lancaCartao(String id, String data, String horasStr) {
        if (id == null || id.isBlank() || data == null || horasStr == null) throw new InvalidDataException("Identificacao do empregado nao pode ser nula.");
        double horas;
        try {
            horas = Double.parseDouble(horasStr.replace(",", "."));
        } catch (NumberFormatException ex) {
            throw new InvalidDataException("Horas deve ser numerica.");
        }
        if (horas <= 0) throw new InvalidDataException("Horas devem ser positivas.");
        // Validação de data: formato dd/MM/yyyy
        if (!wepayu.util.DateUtils.isValidDate(data)) {
            throw new InvalidDataException("Data invalida.");
        }
        Employee e = PayrollDatabase.getEmployee(id);
        if (e == null) throw new EmployeeNotFoundException("Empregado nao existe.");
        if (!(e instanceof HourlyEmployee)) throw new InvalidDataException("Empregado nao eh horista.");
        ((HourlyEmployee) e).addTimeCard(new TimeCard(data, horas));
    }

    public void lancaVenda(String id, String data, double valor) {
        if (id == null || id.isBlank() || data == null) throw new InvalidDataException("Identificacao do empregado nao pode ser nula.");
        if (!wepayu.util.DateUtils.isValidDate(data)) throw new InvalidDataException("Data invalida.");
        double valorCorrigido = valor;
        // Trata valores com vírgula vindos como String convertida incorretamente
        if (String.valueOf(valor).contains(",")) {
            try {
                valorCorrigido = Double.parseDouble(String.valueOf(valor).replace(",", "."));
            } catch (NumberFormatException ex) {
                throw new InvalidDataException("Valor deve ser numerico.");
            }
        }
        if (valorCorrigido <= 0) throw new InvalidDataException("Valor deve ser positivo.");
        Employee e = PayrollDatabase.getEmployee(id);
        if (e == null) throw new EmployeeNotFoundException("Empregado nao existe.");
        if (!(e instanceof CommissionedEmployee)) throw new InvalidDataException("Empregado nao eh comissionado.");
        ((CommissionedEmployee) e).addSalesReceipt(new SalesReceipt(data, valorCorrigido));

    }
    // Sobrecarga para aceitar valor como String (com vírgula)
    public void lancaVenda(String id, String data, String valorStr) {
        if (id == null || id.isBlank() || data == null || valorStr == null) throw new InvalidDataException("Identificacao do empregado nao pode ser nula.");
        if (!wepayu.util.DateUtils.isValidDate(data)) throw new InvalidDataException("Data invalida.");
        double valor;
        try {
            valor = Double.parseDouble(valorStr.replace(",", "."));
        } catch (NumberFormatException ex) {
            throw new InvalidDataException("Valor deve ser numerico.");
        }
        if (valor <= 0) throw new InvalidDataException("Valor deve ser positivo.");
        lancaVenda(id, data, valor);
    // ...existing code...
    }

    public void lancaTaxaServico(String id, String data, double valor) {
    // Sobrecarga para aceitar valor como String (com vírgula)
        if (id == null || id.isBlank()) throw new InvalidDataException("Identificacao do membro nao pode ser nula.");
        if (!wepayu.util.DateUtils.isValidDate(data)) throw new InvalidDataException("Data invalida.");
        double valorCorrigido = valor;
        // Trata valores com vírgula vindos como String convertida incorretamente
        if (String.valueOf(valor).contains(",")) {
            try {
                valorCorrigido = Double.parseDouble(String.valueOf(valor).replace(",", "."));
            } catch (NumberFormatException ex) {
                throw new InvalidDataException("Valor deve ser numerico.");
            }
        }
        if (valorCorrigido <= 0) throw new InvalidDataException("Valor deve ser positivo.");
        Employee e = null;
        for (Employee emp : PayrollDatabase.getAllEmployees().values()) {
            if (emp.getUnionMembership() != null && id.equals(emp.getUnionMembership().getUnionId())) {
                e = emp;
                break;
            }
        }
        if (e == null) throw new InvalidDataException("Membro nao existe.");
        e.getUnionMembership().addServiceCharge(new ServiceCharge(data, valorCorrigido));
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
