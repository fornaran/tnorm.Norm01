package tnorm;

import java.util.*;

public class Main {

    private final static Ontology ontology = new Ontology();
    private final static Scanner scanner = new Scanner(System.in);

    public static void main( String[] args ) {

        ontology.createOntology();
        ontology.printOntology();
        Ontology.setNow("2020-05-21T12:00:00Z");
        ontology.updateOntology();
        ontology.printOntology();
        Ontology.setNow("2020-06-01T23:00:00Z");
        ontology.updateOntology();
        ontology.printOntology();

        String input = askInput();
        while(!input.equals("q")){
            ontology.updateOntology();
            ontology.printOntology();
            input = askInput();
        }
    }



    private static String askInput () {
        scanner.reset();
        String time;
        String vehicle;
        System.out.print("Type your move (h for help): ");
        String input = scanner.nextLine();

        switch (input){
            case "a":
                System.out.println("\n========== Restricted Traffic Area Access ==========");
                System.out.print("Vehicle: ");
                vehicle = scanner.nextLine();
                time = getTime();
                ontology.addRestrictTrafficAreaAccess(time, vehicle);
                System.out.println("====================================================");
                break;
            case "n":
                System.out.println("\n========== Set Now Time Instant =================");
                time = getTime();
                Ontology.setNow(time);
                System.out.println("====================================================");
                break;
            case "p":
                ontology.printRestrictedTrafficAreaAccesses();
                System.out.print("Number of the access to pay for: ");
                int access = Integer.parseInt(scanner.nextLine());
                System.out.println("\n========== Add Payment =========================");
                time = getTime();
                ontology.addPayment(time, access);
                System.out.println("====================================================");
                break;
            case "h":
                System.out.println("\n========== Possible Inputs ======================");
                System.out.println("\ta --> Insert new Restricted Traffic Area Access");
                System.out.println("\tn --> Update Now");
                System.out.println("\tp --> Add payment");
                System.out.println("\tq --> Quit");
                System.out.println("====================================================");
                break;
        }

        System.out.print("\nPress Enter to continue");
        scanner.nextLine();

        return input;

    }

    private static String getTime(){
        System.out.print("Day: ");
        String day = scanner.nextLine();
        System.out.print("Month: ");
        String month = scanner.nextLine();
        System.out.print("Year: ");
        String year = scanner.nextLine();
        System.out.print("Hour: ");
        String hour = scanner.nextLine();
        System.out.print("Minute: ");
        String minute = scanner.nextLine();
        if(month.length() == 1){
            month = "0" + month;
        }
        if(day.length() == 1){
            day = "0" + day;
        }
        if(hour.length() == 1){
            hour = "0" + hour;
        }
        if(minute.length() == 1){
            minute = "0" + minute;
        }
        return year + "-" + month + "-" + day + "T" + hour + ":" + minute + ":00Z";
    }

}
