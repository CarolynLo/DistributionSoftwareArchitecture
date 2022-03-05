/**
 * @(#)ClientOutput.java
 *
 * Copyright: Copyright (c) 2003 Carnegie Mellon University
 *
 */


import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

 /**
 * Event handler of this logging output component.
  * Write all the output into a log file
 * @param event an event object. (caution: not to be directly referenced)
 * @param param a parameter object of the event. (to be cast to appropriate data type)
 */

public class LoggingOutput implements Observer {
    FileHandler fileHandler;
    Logger logger;
     /**
      * Constructs a logging output component. A new client output component subscribes to show events
      * at the time of creation.
      * Then, when we output something in console, we can also write into a log file.
      */
    public LoggingOutput() throws IOException {
        // Subscribe to SHOW event.
        // When EV_SHOW happens, we also need to log.
        EventBus.subscribeTo(EventBus.EV_SHOW, this);
        fileHandler = new FileHandler("Output.log");
        this.fileHandler.setFormatter(new SimpleFormatter());
        this.logger = Logger.getLogger(getClass().toString());
        this.logger.setUseParentHandlers(false);
        this.logger.addHandler(this.fileHandler);
    }

    /**
     * Write the output into the log file
     * @param event an event object. (caution: not to be directly referenced)
     * @param param a parameter object of the event. (to be cast to appropriate data type)
     */
    public void update(Observable event, Object param) {
            logger.info(param.toString());
    }

}
