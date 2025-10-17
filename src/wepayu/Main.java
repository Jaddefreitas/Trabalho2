package wepayu;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        String facade = "wepayu.facade.PayrollFacade";

        // Debug mode: java wepayu.Main --debug-payroll <date>
        if (args.length == 2 && "--debug-payroll".equals(args[0])) {
            String date = args[1];
            System.out.println("DEBUG: running payroll for date " + date);
            java.util.List<wepayu.model.Paycheck> checks = wepayu.service.PayrollService.runPayroll(date);
            java.math.BigDecimal total = java.math.BigDecimal.ZERO;
            int i = 0;
            for (wepayu.model.Paycheck pc : checks) {
                i++;
                java.math.BigDecimal gross = java.math.BigDecimal.valueOf(pc.getGrossPay());
                java.math.BigDecimal ded = java.math.BigDecimal.valueOf(pc.getDeductions());
                java.math.BigDecimal net = gross.subtract(ded).setScale(2, java.math.RoundingMode.HALF_UP);
                total = total.add(net);
                wepayu.model.Employee e = wepayu.service.PayrollDatabase.getEmployee(pc.getEmployeeId());
                String name = (e == null) ? "<unknown>" : e.getName();
                System.out.println(String.format("%d) id=%s name=%s gross=%s ded=%s net=%s", i, pc.getEmployeeId(), name, gross.toPlainString(), ded.toPlainString(), net.toPlainString()));
            }
            System.out.println("TOTAL_NET=" + total.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            return;
        }

        // Replay a focused subset of tests/us9.txt in-process so we can inspect intermediate state
        if (args.length == 1 && "--replay-us9".equals(args[0])) {
            wepayu.facade.PayrollFacade f = new wepayu.facade.PayrollFacade();
            // Case 4 from tests/us9: salaried employee weekly 5
            f.zerarSistema();
            String id2 = f.criarEmpregado("Beta Dois Assalariado", "Rua Beta, 2 - Esparta", "assalariado", "2800");
            f.alteraEmpregado(id2, "agendaPagamento", "semanal 5");
            // inspect payroll for the sequence of dates used in the test
            String[] dates = new String[]{"1/1/2005", "7/1/2005", "14/1/2005", "21/1/2005", "28/1/2005", "31/1/2005"};
            for (String d : dates) {
                System.out.println("--- payroll for " + d + " ---");
                java.util.List<wepayu.model.Paycheck> checks = wepayu.service.PayrollService.runPayroll(d);
                java.math.BigDecimal total = java.math.BigDecimal.ZERO;
                for (wepayu.model.Paycheck pc : checks) {
                    java.math.BigDecimal gross = java.math.BigDecimal.valueOf(pc.getGrossPay());
                    java.math.BigDecimal ded = java.math.BigDecimal.valueOf(pc.getDeductions());
                    java.math.BigDecimal net = gross.subtract(ded).setScale(2, java.math.RoundingMode.HALF_UP);
                    System.out.println("id=" + pc.getEmployeeId() + " gross=" + gross.toPlainString() + " ded=" + ded.toPlainString() + " net=" + net.toPlainString());
                    total = total.add(net);
                }
                System.out.println("TOTAL_NET=" + total.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            }
            return;
        }

        // Replay the two-week (semanal 2 5) salaried scenario from tests/us9
        if (args.length == 1 && "--replay-us9-2w".equals(args[0])) {
            wepayu.facade.PayrollFacade f = new wepayu.facade.PayrollFacade();
            f.zerarSistema();
            String id2 = f.criarEmpregado("Beta Dois Assalariado", "Rua Beta, 2 - Esparta", "assalariado", "2800");
            f.alteraEmpregado(id2, "agendaPagamento", "semanal 2 5");
            String[] dates2 = new String[]{"1/1/2005", "7/1/2005", "14/1/2005", "21/1/2005", "28/1/2005", "31/1/2005"};
            for (String d : dates2) {
                System.out.println("--- payroll(2w) for " + d + " ---");
                java.util.List<wepayu.model.Paycheck> checks = wepayu.service.PayrollService.runPayroll(d);
                java.math.BigDecimal total = java.math.BigDecimal.ZERO;
                for (wepayu.model.Paycheck pc : checks) {
                    java.math.BigDecimal gross = java.math.BigDecimal.valueOf(pc.getGrossPay());
                    java.math.BigDecimal ded = java.math.BigDecimal.valueOf(pc.getDeductions());
                    java.math.BigDecimal net = gross.subtract(ded).setScale(2, java.math.RoundingMode.HALF_UP);
                    System.out.println("id=" + pc.getEmployeeId() + " gross=" + gross.toPlainString() + " ded=" + ded.toPlainString() + " net=" + net.toPlainString());
                    total = total.add(net);
                }
                System.out.println("TOTAL_NET=" + total.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            }
            return;
        }

        // Replay the full us7 scenario programmatically to inspect generated folha files
        if (args.length == 1 && "--replay-us7".equals(args[0])) {
            wepayu.facade.PayrollFacade f = new wepayu.facade.PayrollFacade();
            f.zerarSistema();

            String id1 = f.criarEmpregado("Fernanda Montenegro", "end1", "horista", "12,50");
            f.alteraEmpregado(id1, "sindicalizado", "false");
            f.alteraEmpregado(id1, "metodopagamento", "emMaos");

            String id2 = f.criarEmpregado("Paloma Duarte", "end2", "horista", "11,25");
            f.alteraEmpregado(id2, "sindicalizado", "false");
            f.alteraEmpregado(id2, "metodopagamento", "banco", "Banco do Brasil", "1591-1", "51000-0");
            f.lancaCartao(id2, "1/1/2005", "8");
            f.lancaCartao(id2, "2/1/2005", "7");
            f.lancaCartao(id2, "1/2/2005", "8");
            f.lancaCartao(id2, "2/2/2005", "7");
            f.lancaCartao(id2, "1/12/2005", "8");
            f.lancaCartao(id2, "2/12/2005", "7");

            String id3 = f.criarEmpregado("Lavinia Vlasak", "end3", "horista", "11,21");
            f.alteraEmpregado(id3, "sindicalizado", "false");
            f.alteraEmpregado(id3, "metodopagamento", "banco", "Banco do Brasil", "1591-1", "51001-1");
            f.lancaCartao(id3, "1/1/2005", "10");
            f.lancaCartao(id3, "2/1/2005", "7");
            f.lancaCartao(id3, "1/2/2005", "8");
            f.lancaCartao(id3, "2/2/2005", "7");
            f.lancaCartao(id3, "1/12/2005", "12");
            f.lancaCartao(id3, "2/12/2005", "7");

            String id4 = f.criarEmpregado("Claudia Abreu", "end4", "horista", "11,00");
            f.alteraEmpregado(id4, "sindicalizado", "true", "s123", "1,00");
            f.alteraEmpregado(id4, "metodopagamento", "emMaos");
            f.lancaCartao(id4, "1/1/2005", "10");
            f.lancaCartao(id4, "2/1/2005", "7");
            f.lancaCartao(id4, "1/2/2005", "8");
            f.lancaCartao(id4, "2/2/2005", "7");
            f.lancaCartao(id4, "1/12/2005", "12");
            f.lancaCartao(id4, "2/12/2005", "7");

            String id5 = f.criarEmpregado("Claudia Raia", "end5", "horista", "10,00");
            f.alteraEmpregado(id5, "sindicalizado", "true", "s124", "1,20");
            f.alteraEmpregado(id5, "metodopagamento", "banco", "Banco do Brasil", "1591-1", "51002-2");
            f.lancaCartao(id5, "1/1/2005", "10");
            f.lancaCartao(id5, "2/1/2005", "7");
            f.lancaCartao(id5, "1/2/2005", "8");
            f.lancaCartao(id5, "2/2/2005", "7");
            f.lancaCartao(id5, "1/12/2005", "12");
            f.lancaCartao(id5, "2/12/2005", "7");
            f.lancaTaxaServico("s124", "1/1/2005", "80");
            f.lancaTaxaServico("s124", "1/12/2005", "80");

            String id6 = f.criarEmpregado("Natalia do Valle", "end6", "assalariado", "1000,00");
            f.alteraEmpregado(id6, "sindicalizado", "false");
            f.alteraEmpregado(id6, "metodopagamento", "correios");

            String id7 = f.criarEmpregado("Regina Duarte", "end7", "assalariado", "1100,00");
            f.alteraEmpregado(id7, "sindicalizado", "true", "s125", "1,00");
            f.alteraEmpregado(id7, "metodopagamento", "correios");

            String id8 = f.criarEmpregado("Flavia Alessandra", "end8", "assalariado", "1200,00");
            f.alteraEmpregado(id8, "sindicalizado", "true", "s126", "1,00");
            f.alteraEmpregado(id8, "metodopagamento", "correios");
            f.lancaTaxaServico("s126", "1/1/2005", "70");
            f.lancaTaxaServico("s126", "1/12/2005", "75");

            String id9 = f.criarEmpregado("Deborah Secco", "end9", "comissionado", "1300,00", "0,11");
            f.alteraEmpregado(id9, "sindicalizado", "false");
            f.alteraEmpregado(id9, "metodopagamento", "correios");

            String id10 = f.criarEmpregado("Ana Paula Arosio", "end10", "comissionado", "1400,00", "0,12");
            f.alteraEmpregado(id10, "sindicalizado", "false");
            f.alteraEmpregado(id10, "metodopagamento", "correios");
            f.lancaVenda(id10, "1/1/2005", "123,45");
            f.lancaVenda(id10, "2/1/2005", "200");
            f.lancaVenda(id10, "1/2/2005", "123,1");
            f.lancaVenda(id10, "2/2/2005", "500");
            f.lancaVenda(id10, "1/12/2005", "600");
            f.lancaVenda(id10, "2/12/2005", "800");

            String id11 = f.criarEmpregado("Suzana Vieira", "end11", "comissionado", "1500,00", "0,13");
            f.alteraEmpregado(id11, "sindicalizado", "true", "s127", "1,00");
            f.alteraEmpregado(id11, "metodopagamento", "correios");
            f.lancaVenda(id11, "1/1/2005", "123,45");
            f.lancaVenda(id11, "2/1/2005", "200");
            f.lancaVenda(id11, "1/2/2005", "123,1");
            f.lancaVenda(id11, "2/2/2005", "500");
            f.lancaVenda(id11, "1/12/2005", "600");
            f.lancaVenda(id11, "2/12/2005", "800");

            String id12 = f.criarEmpregado("Maite Proenca", "end12", "comissionado", "1600,00", "0,14");
            f.alteraEmpregado(id12, "sindicalizado", "true", "s128", "1,00");
            f.alteraEmpregado(id12, "metodopagamento", "correios");
            f.lancaVenda(id12, "1/1/2005", "123,45");
            f.lancaVenda(id12, "2/1/2005", "200");
            f.lancaVenda(id12, "1/2/2005", "123,1");
            f.lancaVenda(id12, "2/2/2005", "500");
            f.lancaVenda(id12, "1/12/2005", "600");
            f.lancaVenda(id12, "2/12/2005", "800");
            f.lancaTaxaServico("s128", "1/1/2005", "70");
            f.lancaTaxaServico("s128", "1/12/2005", "75");

            // Now replicate the rodaFolha checks in the test file for specific dates
            String[] folhaDates = new String[]{"7/1/2005","14/1/2005","21/1/2005","28/1/2005","31/1/2005","4/2/2005","11/2/2005","25/2/2005","28/2/2005"};
            for (String d : folhaDates) {
                System.out.println("\n=== rodaFolha for " + d + " ===");
                // print breakdown
                java.util.List<wepayu.model.Paycheck> checks = wepayu.service.PayrollService.runPayroll(d);
                java.math.BigDecimal total = java.math.BigDecimal.ZERO;
                for (wepayu.model.Paycheck pc : checks) {
                    java.math.BigDecimal gross = java.math.BigDecimal.valueOf(pc.getGrossPay());
                    java.math.BigDecimal ded = java.math.BigDecimal.valueOf(pc.getDeductions());
                    java.math.BigDecimal net = gross.subtract(ded).setScale(2, java.math.RoundingMode.HALF_UP);
                    wepayu.model.Employee e = wepayu.service.PayrollDatabase.getEmployee(pc.getEmployeeId());
                    String name = (e == null) ? "<unknown>" : e.getName();
                    System.out.println("id=" + pc.getEmployeeId() + " name=" + name + " gross=" + gross.toPlainString() + " ded=" + ded.toPlainString() + " net=" + net.toPlainString());
                    total = total.add(net);
                }
                System.out.println("TOTAL_NET=" + total.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());

                // write file using facade and print differences against ok file if present
                // produce file name in ok fixture format: folha-YYYY-MM-DD.txt
                String[] parts = d.split("/");
                String outName = "folha-" + parts[2] + "-" + String.format("%02d", Integer.parseInt(parts[1])) + "-" + String.format("%02d", Integer.parseInt(parts[0])) + ".txt";
                f.rodaFolha(d, outName);
                System.out.println("Wrote " + outName);
                java.io.File okf = new java.io.File("ok/" + outName);
                java.io.File genf = new java.io.File(outName);
                if (okf.exists() && genf.exists()) {
                    System.out.println("--- diff vs ok/" + outName + " ---");
                    try (java.io.BufferedReader ro = new java.io.BufferedReader(new java.io.FileReader(okf));
                         java.io.BufferedReader rg = new java.io.BufferedReader(new java.io.FileReader(genf))) {
                        String lo, lg;
                        int ln = 1;
                        while (true) {
                            lo = ro.readLine();
                            lg = rg.readLine();
                            if (lo == null && lg == null) break;
                            if (lo == null) lo = "<EOF>";
                            if (lg == null) lg = "<EOF>";
                            if (!lo.equals(lg)) {
                                System.out.println("Line " + ln + "\n  expected: " + lo + "\n  actual:   " + lg);
                            }
                            ln++;
                        }
                    } catch (Exception ex) {
                        System.out.println("Error comparing files: " + ex.getMessage());
                    }
                } else {
                    System.out.println("Ok fixture or generated file missing: ok/" + outName + " or " + outName);
                }
            }
            return;
        }

    // Group tests that depend on shared state so they run inside the same JVM process.
    String[][] groups = new String[][]{
        {"tests/us1.txt", "tests/us1_1.txt"},
        {"tests/us2.txt", "tests/us2_1.txt"},
        {"tests/us3.txt", "tests/us3_1.txt"},
        {"tests/us4.txt", "tests/us4_1.txt"},
        {"tests/us5.txt", "tests/us5_1.txt"},
        {"tests/us6.txt", "tests/us6_1.txt"},
        {"tests/us7.txt"},
        {"tests/us8.txt"},
        {"tests/us9.txt", "tests/us9_1.txt"},
        {"tests/us10.txt", "tests/us10_1.txt"}
    };

        String javaCmd = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String cp = "lib" + File.separator + "easyaccept.jar" + System.getProperty("path.separator") + "bin";

        for (String[] group : groups) {
            String groupName = String.join(",", group);
            System.out.println("\n=== Running group: " + groupName + " ===");

            List<String> cmd = new ArrayList<>();
            cmd.add(javaCmd);
            cmd.add("-cp");
            cmd.add(cp);
            cmd.add("easyaccept.EasyAccept");
            cmd.add(facade);
            for (String test : group) cmd.add(test);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exit = p.waitFor();
            System.out.println("Process exit code: " + exit + " for group: " + groupName);
        }
    }
}