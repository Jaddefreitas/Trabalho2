package wepayu.model;

public class Sale {
    private String data;
    private double valor;

    public Sale(String data, double valor) {
        this.data = data;
        this.valor = valor;
    }

    public String getData() {
        return data;
    }

    public double getValor() {
        return valor;
    }
}


