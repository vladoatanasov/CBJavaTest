package com.company;

import com.couchbase.lite.*;
import com.couchbase.lite.internal.database.log.SystemLogger;
import com.couchbase.lite.replicator.Replication;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static void main(String[] args) {
        String databaseName = "default";
        Database database;

        Manager manager;

//        Manager.enableLogging(TAG, com.couchbase.lite.util.Log.VERBOSE);
//        Manager.enableLogging(com.couchbase.lite.util.Log.TAG, com.couchbase.lite.util.Log.VERBOSE);
//        Manager.enableLogging(com.couchbase.lite.util.Log.TAG_SYNC_ASYNC_TASK, com.couchbase.lite.util.Log.VERBOSE);
//        Manager.enableLogging(com.couchbase.lite.util.Log.TAG_SYNC, com.couchbase.lite.util.Log.VERBOSE);
//        Manager.enableLogging(com.couchbase.lite.util.Log.TAG_QUERY, com.couchbase.lite.util.Log.VERBOSE);
//        Manager.enableLogging(com.couchbase.lite.util.Log.TAG_VIEW, com.couchbase.lite.util.Log.VERBOSE);
//        Manager.enableLogging(com.couchbase.lite.util.Log.TAG_DATABASE, com.couchbase.lite.util.Log.VERBOSE);

        if (!Manager.isValidDatabaseName(databaseName)) {
            LOGGER.log(Level.SEVERE, "Bad database name");
            return;
        }

        try {
            manager = new Manager(new JavaContext(), Manager.DEFAULT_OPTIONS);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Cannot create manager object");
            return;
        }

        try {
            DatabaseOptions options = new DatabaseOptions();
            options.setCreate(true);
            options.setStorageType(Manager.SQLITE_STORAGE);
            database = manager.openDatabase(databaseName, options);

            URL url = null;

            try {
                url = new URL("http://192.168.99.1:4984/default");
            } catch(MalformedURLException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
            }

            Replication replication = database.createPullReplication(url);
            replication.setChannels(Arrays.asList(new String[]{"ref_data_v1"}));
            replication.setContinuous(false);

            long timestamp = System.currentTimeMillis();

            replication.addChangeListener(changeEvent -> {
                long delta = (System.currentTimeMillis() - timestamp) / 1000;

                long threshold = 0;
                int completedDocs = changeEvent.getCompletedChangeCount();
                if (delta > 0) {
                    threshold = completedDocs / delta;
                }
                System.out.printf("%d docs in %d sec (%d doc/sec)\n", completedDocs, delta, threshold);
            });

            replication.start();
            System.out.println("Replication started");

        } catch (CouchbaseLiteException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }

    }


}
