package isobuilder.examples.kompliziert;

import javax.annotation.Generated;

@Generated("von Hand")
public class BobBuilder_Contract {
    interface KevinStep {
        ChantalStep kevin(String kevin);
    }
    interface ChantalStep {
        JustinStep chantal(String chantal);
    }
    interface JustinStep {
        Modder justin(String justin);
    }
    interface Modder {
        Modder updateKevin(String kevin);
        Modder updateChantal(String chantal);
        Modder updateJustin(String justin);
        Bob build();
    }
}
