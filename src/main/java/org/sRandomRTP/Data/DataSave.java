package org.sRandomRTP.Data;

import org.sRandomRTP.DifferentMethods.Variables;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class DataSave {
    public static void dataSave() {
        File file = new File(Variables.getInstance().getDataFolder(), "Data/rtpCount.yml");
        int uses = Variables.rtpCount.getOrDefault(1, 0);
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("Uses: " + uses);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}