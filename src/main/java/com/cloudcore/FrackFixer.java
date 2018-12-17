package com.cloudcore;

import com.cloudcore.desktop.core.CloudCoin;
import com.cloudcore.desktop.core.Config;
import com.cloudcore.desktop.core.FileSystem;
import com.cloudcore.desktop.raida.FixitHelper;
import com.cloudcore.desktop.raida.Node;
import com.cloudcore.desktop.raida.RAIDA;
import com.cloudcore.desktop.raida.Response;
import com.cloudcore.desktop.utils.*;
import com.cloudcore.desktop.utils.CoinUtils;
import com.cloudcore.desktop.utils.FileUtils;
import com.cloudcore.desktop.utils.SimpleLogger;
import com.google.gson.Gson;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;


import java.util.ArrayList;

public class FrackFixer {
    /* Fields */

    public static SimpleLogger logger;
    public String RootPath = FileSystem.RootPath;
    private RAIDA raida;

    public boolean continueExecution = true;
    public boolean isFixing = false;


    /* Constructors */

    public FrackFixer(String path) {
        RootPath = path;
        System.out.println(path);
        raida = RAIDA.getInstance();
    }


    public void multiFix() {
        final File folder = new File(FileSystem.FrackedFolder);
        int i=0;
        ArrayList<CloudCoin> folderCoins = new ArrayList<CloudCoin>();

        for (final File fileEntry : folder.listFiles()) {
            if (!fileEntry.isDirectory()) {
                System.out.println(fileEntry.getName());
                ArrayList<CloudCoin> fileCoins =  FileUtils.loadCloudCoinsFromStack(FileSystem.FrackedFolder , fileEntry.getName());
                i++;
                for (CloudCoin coin:fileCoins
                     ) {
                    folderCoins.add(coin);
                }
               // folderCoins.add(fileCoins);
            }
        }

        ArrayList<CloudCoin> frackCoinsAtPosition= Utils.GetFrackedCoins(folderCoins,1);
        System.out.println("Fracked Coins at position " + 0 + "- " + frackCoinsAtPosition.size());

        for(int j=0; j < 25; j++) {
      //      ArrayList<CloudCoin> frackCoinsAtPosition= Utils.GetFrackedCoins(folderCoins,j);
       //     System.out.println("Fracked Coins at position " + j + "- " + frackCoinsAtPosition.size());
        }
        System.out.println("Total files parsed: " + i+ " CloudCoins Loaded: " + folderCoins.size());

    }
    /* Methods */

    public boolean fixOneGuidCorner(int raida_ID, CloudCoin cc, int corner, int[] triad) {
        if (raida.nodes[raida_ID].isFailed() || raida.nodes[raida_ID].failsFix) {
            String response = "RAIDA Fails Echo or Fix. Try again when RAIDA online.";
            updateLog(response);
            return false;
        } else {
            if (!raida.nodes[triad[0]].isFailed() || !raida.nodes[triad[1]].isFailed() || !raida.nodes[triad[2]].isFailed()) {
                String[] ans = {cc.getAn().get(triad[0]), cc.getAn().get(triad[1]), cc.getAn().get(triad[2])};
                raida.getTickets(triad, ans, cc.getNn(), cc.getSn(), CoinUtils.getDenomination(cc), Config.milliSecondsToTimeOut);

                if (raida.nodes[triad[0]].hasTicket && raida.nodes[triad[1]].hasTicket && raida.nodes[triad[2]].hasTicket) {
                    if (!continueExecution) {
                        updateLog("Aborting Fix for new operation.");
                        return false;
                    }
                    Response fixResponse = RAIDA.getInstance().nodes[raida_ID].fix(triad, raida.nodes[triad[0]].ticket, raida.nodes[triad[1]].ticket, raida.nodes[triad[2]].ticket, cc.getAn().get(raida_ID));
                    if (fixResponse.success) {
                        updateLog("RAIDA" + raida_ID + " unfracked successfully.");
                        return true;
                    } else {
                        updateLog("RAIDA failed to accept tickets on corner " + corner);
                        return false;
                    }
                } else {
                    updateLog("Trusted servers failed to provide tickets for corner " + corner);
                    return false;
                }
            }
            updateLog("One or more of the trusted triad will not echo and detect. So not trying.");
            return false;
        }
    }


    /* PUBLIC METHODS */

    public int[] fixAll() {
        int totalValueToBank = 0, totalValueToFractured = 0, totalValueToCounterfeit = 0;
        isFixing = continueExecution = true;
        int[] results = new int[3];
        ArrayList<CloudCoin> coins = FileSystem.loadFolderCoins(FileSystem.FrackedFolder);

        if (coins.size() <= 0)
            updateLog("You have no fracked coins.");

        updateLog("Fixing fracked coins: " + coins.size());
        for (int i = 0; i < coins.size(); i++) {
            if (!continueExecution) {
                updateLog("Aborting Fix 1");
                break;
            }
            updateLog("Unfracking coin " + (i + 1) + " of " + coins.size());

            CloudCoin coin = coins.get(i);
            if (coin == null) {
                updateLog("CloudCoin " + i + " is null, skipping");
                continue;
            }
           // CoinUtils.consoleReport(coin);
            coin = fixCoin(coin);
            coins.set(i, coin); // TODO: get rid of safety check
            if (!continueExecution) {
                updateLog("Aborting Fix 2");
                break;
            }
           // CoinUtils.consoleReport(coin);

            if (FileSystem.BankFolder.equals(coin.folder)) {
                totalValueToBank++;
                updateLog("CloudCoin was moved to Bank.");
            }
            else if (FileSystem.CounterfeitFolder.equals(coin.folder)) {
                totalValueToCounterfeit++;
                updateLog("CloudCoin was moved to Counterfeit.");
            }
            else {
                totalValueToFractured++;
                updateLog("CloudCoin was moved back to Fracked folder.");
            }
            //FileSystem.moveCoin(coin, FileSystem.FrackedFolder, coin.folder);
        }

        results[0] = totalValueToBank;
        results[1] = totalValueToCounterfeit; // System.out.println("Counterfeit and Moved to trash: "+totalValueToCounterfeit);
        results[2] = totalValueToFractured; // System.out.println("Fracked and Moved to Fracked: "+ totalValueToFractured);
        isFixing = false;
        continueExecution = true;
        updateLog("Finished Frack Fixing. Fixed " + totalValueToBank + " CloudCoins and moved them into Bank Folder");
        return results;
    }


    public CloudCoin fixCoin(CloudCoin coin) {
      FixitHelper fixer;

        for (Node node : RAIDA.getInstance().nodes) node.resetTicket();
        for (Node node : RAIDA.getInstance().nodes) node.newCoin();

        long before = System.currentTimeMillis();

        int corner;
        for (int i = 0; i < 25; i++) {
            if (!continueExecution) {
                System.out.println("Stopping Execution");
                return coin;
            }

            if (!"pass".equals(CoinUtils.getPastStatus(coin, i))) {
                updateLog("Attempting to fix RAIDA " + i);

                fixer = new FixitHelper(i, coin.getAn().toArray(new String[0]));

                corner = 1;
                while (!fixer.finished) {
                    if (!continueExecution) {
                        System.out.println("Stopping Execution");
                        return coin;
                    }
                    updateLog("Using corner " + corner + " Pown is " + coin.getPown());
                    if (fixOneGuidCorner(i, coin, corner, fixer.currentTriad)) {
                        CoinUtils.setPastStatus(coin, "pass", i);
                        fixer.finished = true;
                        corner = 1;
                    } else {
                        corner++;
                        fixer.setCornerToCheck(corner);
                    }
                }
            }
        }

        for (int raida_ID = 24; raida_ID > 0; raida_ID--) {
            if (!continueExecution) return coin;

            if (!"pass".equals(CoinUtils.getPastStatus(coin, raida_ID))) {
                updateLog("Attempting to fix RAIDA " + raida_ID);

                fixer = new FixitHelper(raida_ID, coin.getAn().toArray(new String[0]));

                corner = 1;
                while (!fixer.finished) {
                    updateLog("Using corner " + corner);
                    if (fixOneGuidCorner(raida_ID, coin, corner, fixer.currentTriad)) {
                        CoinUtils.setPastStatus(coin, "pass", raida_ID);
                        fixer.finished = true;
                        corner = 1;
                    } else {
                        corner++;
                        fixer.setCornerToCheck(corner);
                    }
                }
            }
        }
        long after = System.currentTimeMillis();
        long ts = after - before;
        updateLog("Time spent fixing RAIDA in milliseconds: " + ts);

        CoinUtils.calcExpirationDate(coin);
        return coin;
    }

    public void updateLog(String message) {
        System.out.println(message);
//        logger.Info(message);
    }

}
