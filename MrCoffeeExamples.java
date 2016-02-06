
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.ExceptionListener;
import javax.swing.event.ChangeListener;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.event.ChangeEvent;

/*
 * (C) Copyright 2016, Damian Nikodem, All Rights Reserved. Mr. Coffee is free
 * software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without Mr. Coffee the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with Mr. Coffee. If not, see http://www.gnu.org/licenses/.
 *
 * This class _IS_ available for closed source projects. Please email
 * damian.nikodem.au@gmail.com to arrange a License.
 * 
 */
public class MrCoffeeExamples {

    public void basicUse() throws Exception {
        /*
        This example simply imports the requested library into the current JVM.
        No user feedback is provided, nor is 
        */
        // Initialize Mr Coffee
        MrCoffee loader = new MrCoffee();

        // Optional step to allow multiple Mr. Coffee Applications to run on the same
        // machine and user account without conflict.
        MrCoffee.setApplicationId("Mr. Coffee Basic-Example Application V1.0");

        // Add a .jar to initialize ( repeat as many times as needed )
        loader.requestJar("http://wiredx.net/jcterm/jsch-0.1.46.jar");

        // Load jar files into current jvm.
        loader.loadBasic();

        // Create instance of Class which may use code from downloaded library
        
    }

    public void advancedUse() throws Exception {
        /*
        This example assumes that you wish to provide the user with visual feedback of
        some form that the application is downloading required components.
        */
        // Initialize Mr Coffee
        MrCoffee loader = new MrCoffee();
        
        // Optional step to allow multiple Mr. Coffee Applications to run on the same
        // machine and user account without conflict.
        MrCoffee.setApplicationId("Mr. Coffee Example Application V1.0");
        
        // Add a .jar to initialize ( repeat as many times as needed )
        loader.requestJar("http://wiredx.net/jcterm/jsch-0.1.46.jar");
        
        // Create UI Class to 
        AdvancedExample advancedExample = new AdvancedExample();
        
        // Initialize ASynchronous downloader and provide listeners for all relevant events.
        Runnable runner = loader.loadAsync(advancedExample, advancedExample.boundedRangeModel, advancedExample);
        
        // Launch thread to handle download process.
        new Thread(runner).start();
        
        // Sleep while the thread runs.
        while (advancedExample.isRunning) {
            Thread.sleep(500);
        }
        
        // Create instance of Class which may use code from downloaded library
    }
}

class AdvancedExample implements ActionListener, ExceptionListener, ChangeListener {

    public BoundedRangeModel boundedRangeModel = new DefaultBoundedRangeModel();
    public boolean isRunning = true;

    public AdvancedExample() {
        boundedRangeModel.addChangeListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getID()) {
            case MrCoffee.EVENT_DOWNLOAD_STARTED:
                System.out.println("Mr. Coffee Download Started");
                break;
            case MrCoffee.EVENT_DOWNLOAD_FINISHED:
                System.out.println("Mr. Coffee Download finished");
                isRunning = false;
                break;
            case MrCoffee.EVENT_DOWNLOAD_EXCEPTION:
                System.out.println("File Download Exception: " + e.getActionCommand());
                break;
            case MrCoffee.EVENT_FILE_STARTED:
                System.out.println("File Download Started: " + e.getActionCommand());
                break;
            case MrCoffee.EVENT_FILE_FINISHED:
                System.out.println("File Download Finished: " + e.getActionCommand());
                break;
        }
    }

    @Override
    public void exceptionThrown(Exception e) {
        e.printStackTrace();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        System.out.println("Downloaded: " + boundedRangeModel.getValue() + " Bytes / " + boundedRangeModel.getMaximum() + " bytes");
    }

}
