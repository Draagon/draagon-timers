/*
 * Copyright 2003 Draagon Software LLC. All Rights Reserved.
 *
 * This software is the proprietary information of Draagon Software LLC.
 * Use is subject to license terms.
 *
 */
package com.draagon.timers;

import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Timer used to calculate stats on method execution time. Instantiate to start
 * timer, and then call the done() method to stop timer. Note: that the
 * ConcurrentHashMap used as a cache is internally optimized for thread safety
 * and locking. It may be safely accessed from multiple threads that share the
 * singleton instance. <p>
 *
 * @author Doug Mealing
 * @author Emil Lefkof
 */
public class MethodTimer implements Comparable {

    private static boolean isOn = true;
    //constants
    public static final int TOTAL_ACCESS_COUNT = 0;
    public static final int TOTAL_AVERAGE = 1;
    public static final int TOTAL_MIN_TIME = 2;
    public static final int TOTAL_MAX_TIME = 3;
    public static final int CURRENT_ACCESS_COUNT = 4;
    public static final int CURRENT_AVERAGE = 5;
    public static final int CURRENT_MIN_TIME = 6;
    public static final int CURRENT_MAX_TIME = 7;
    //static map to store all timers
    private static final Map cGroupings = new ConcurrentHashMap();
    private static final Map cPackages = new ConcurrentHashMap();
    //private fields
    private boolean mDoneFlag;
    public long mAccessCount_g;
    public long mServiceTimeSum_g;
    public long mServiceTimeSumOfSquares_g;
    public long mServiceTimeMin_g = 0;
    public long mServiceTimeMax_g = 0;
    public long mAccessCount = 0;
    public long mServiceTimeSum = 0;
    public long mServiceTimeSumOfSquares = 0;
    public long mServiceTimeMin = -1;
    public long mServiceTimeMax = -1;
    private String mPackage;
    private String mClass;
    private String mName;
    private String description;
    private String author;
    private static ThreadLocal<Object> groupingThreadLocal = new ThreadLocal<Object>();

    /**
     * Used to group method timers together in logical groupings
     */
    public static void setThreadGrouping(Object grouping) {
        groupingThreadLocal.set(grouping);
    }
    private static MethodTimer mDummyTimer = new MethodTimer();

    /**
     * Retrieves the timer for object and name. <p>
     *
     * @param aObject object to create timer for
     * @param aName name to give this timer (could be method name) @returns A
     * Method Timer object
     */
    public static MethodTimer getTimer(Object aObject, String aName) {
        if (!isOn()) {
            return mDummyTimer;
        }
        return new MethodTimer(aObject.getClass(), aName);
    }

    /**
     * Retrieves the timer for object and name. <p>
     *
     * @param aObject object to create timer for
     * @param aName name to give this timer (could be method name)
     * @param desc the description of the timer
     * @param author the author of timed code @returns A Method Timer object
     */
    public static MethodTimer getTimer(Object aObject, String aName, String desc, String author) {
        if (!isOn()) {
            return mDummyTimer;
        }

        MethodTimer t = new MethodTimer(aObject.getClass(), aName);
        t.setDescription(desc);
        t.setAuthor(author);
        return t;
    }

    /**
     * Retrieves the timer for object and name. <p>
     *
     * @param aClass class to create timer for
     * @param aName name to give this timer (could be method name) @returns A
     * Method Timer object
     */
    public static MethodTimer getTimer(Class aClass, String aName) {
        if (!isOn()) {
            return mDummyTimer;
        }
        return new MethodTimer(aClass, aName);
    }

    /**
     * Retrieves the timer for object and name. <p>
     *
     * @param aClass class to create timer for
     * @param aName name to give this timer (could be method name)
     * @param desc the description of the timer
     * @param author the author of timed code @returns A Method Timer object
     */
    public static MethodTimer getTimer(Class aClass, String aName, String desc, String author) {
        if (!isOn()) {
            return mDummyTimer;
        }

        MethodTimer t = new MethodTimer(aClass, aName);
        t.setDescription(desc);
        t.setAuthor(author);
        return t;
    }

    /**
     * Contructor starts the timer for object and name. <p>
     *
     * @param aObject object to create timer for
     * @param aName name to give this timer (could be method name)
     */
    public MethodTimer(Object aObject, String aName) {
        this(aObject.getClass(), aName);
    }

    /**
     * Contructor starts the timer for object and name. <p>
     *
     * @param aObject object to create timer for
     * @param aName name to give this timer (could be method name)
     * @param startTime The time to use for the start of the timer
     */
    public MethodTimer(Object aObject, String aName, long startTime) {
        this(aObject.getClass(), aName, startTime);
    }

    /**
     * Contructor starts the timer for class and name. <p>
     *
     * @param aClass the class to create the timer for
     * @param aName name to give this timer (could be method name)
     */
    public MethodTimer(Class aClass, String aName) {

        String tmp = aClass.getName();
        int i = tmp.lastIndexOf('.');

        if (i >= 0) {
            mClass = tmp.substring(i + 1);
            mPackage = tmp.substring(0, i);
        } else {
            mPackage = "";
            mClass = tmp;
        }

        mName = aName;
        mServiceTimeSum = System.currentTimeMillis();
    }

    /**
     * Contructor starts the timer for class and name. <p>
     *
     * @param aClass the class to create the timer for
     * @param aName name to give this timer (could be method name)
     * @param startTime The time to use for the start of the timer
     */
    public MethodTimer(Class aClass, String aName, long startTime) {
        this(aClass, aName);

        mServiceTimeSum = startTime;
    }

    /**
     * Don't allow unparameterized construction.
     */
    private MethodTimer() {
    }

    /**
     * Turn on the Method Timers
     */
    public static void setOn() {
        isOn = true;
    }

    /**
     * Turn the Method Timers off
     */
    public static void setOff() {
        isOn = false;

        // Clear all the timers
        cPackages.clear();
        cGroupings.clear();
    }

    /**
     * Whether the Method Timers are on
     */
    public static boolean isOn() {
        return isOn;
    }

    /**
     * Gets a collection of all unique package names. <p>
     *
     * @return collection of all package names
     */
    public static Collection getAllGroupings() {

        //make a shallow thread-safe copy of the keys
        TreeSet packageSet = new TreeSet(cGroupings.keySet());

        return packageSet;
    }

    public static String getGroupKey(Object o) {
        if (o == null) {
            return null;
        }
        int h = o.hashCode();
        return Integer.toString(h, 16);
    }

    /**
     * Gets a collection of all unique package names. <p>
     *
     * @return collection of all package names
     */
    public static Collection getAllPackages() {

        //make a shallow thread-safe copy of the keys
        TreeSet packageSet = new TreeSet(cPackages.keySet());

        return packageSet;
    }

    /**
     * Gets a copy of all of the classes for a package listed by package name
     * aPackage. <p>
     *
     * @param aPackage the name of the package to look for
     * @return collection of all class names for package if empty collection
     * then no package found
     */
    public static Collection getAllPackages(String groupKey) {

        TreeSet packageSet = new TreeSet();

        int key = Integer.parseInt(groupKey, 16);

        Map packageMap = null;
        for (Object o : cGroupings.keySet()) {
            if (o.hashCode() == key) {
                packageMap = (Map) cGroupings.get(o);
            }
        }

        //if key not found return an empty collection
        if (packageMap == null) {
            return packageSet;
        }

        //make a sorted copy of the keys
        packageSet.addAll(packageMap.keySet());

        return packageSet;
    }

    /**
     * Gets a copy of all of the classes for a package listed by package name
     * aPackage. <p>
     *
     * @param aPackage the name of the package to look for
     * @return collection of all class names for package if empty collection
     * then no package found
     */
    public static Collection getAllClasses(String aPackage) {

        TreeSet classSet = new TreeSet();

        //if key not found return an empty collection
        if (!cPackages.containsKey(aPackage)) {

            return classSet;
        }

        Map classMap = (Map) cPackages.get(aPackage);

        //make a sorted copy of the keys
        classSet.addAll(classMap.keySet());

        return classSet;
    }

    /**
     * Gets a collection of all unique timers. <p>
     *
     * @param aPackage the name of the package
     * @param aClass the name of the class
     * @return collection of the timer data
     */
    public static Collection getAllTimers(String aPackage,
            String aClass) {
        TreeSet methodSet = new TreeSet();

        //if key not found return an empty collection
        if (!cPackages.containsKey(aPackage)) {
            return methodSet;
        }

        //get the classes for a package name
        Map classMap = (Map) cPackages.get(aPackage);

        //if key not found return an empty collection
        if (!classMap.containsKey(aClass)) {
            return methodSet;
        }

        //get the methods for a class name
        Map methodMap = (Map) classMap.get(aClass);

        //get the sorted method timers values
        methodSet.addAll(methodMap.values());

        return methodSet;
    }

    /**
     * Gets the name property. <p>
     *
     * @return sets the name property.
     */
    public String getName() {

        return mName;
    }

    /**
     * Calculate the averge time. <p>
     *
     * @param aTimer the method time to calulcate the average for
     * @return average in milliseconds
     */
    public static long average(MethodTimer aTimer) {

        // the way we do things, mAccessCount will always be 1 or more
        if (aTimer.mAccessCount == 0) {

            return 0;
        }

        return aTimer.mServiceTimeSum / aTimer.mAccessCount;
    }

    /**
     * Calc average milliseconds between two sample times. <p>
     *
     * @param aTimer2 a method timer
     * @param aTimer1 a method timer
     * @return the average in millseconds.
     */
    public static long average(MethodTimer aTimer2,
            MethodTimer aTimer1) {

        if (aTimer2.mAccessCount <= aTimer1.mAccessCount) {

            // pretend access count difference  was 1
            return (aTimer2.mServiceTimeSum - aTimer1.mServiceTimeSum);
        }

        return (aTimer2.mServiceTimeSum - aTimer1.mServiceTimeSum) / (aTimer2.mAccessCount - aTimer1.mAccessCount);
    }

    /**
     * Calc average milliseconds over all time. <p>
     *
     * @param aTimer the Method Timer
     * @return the average in millseconds.
     */
    public static long average_g(MethodTimer aTimer) {

        // the way we do things, mAccessCount will always be 1 or more
        if (aTimer.mAccessCount_g == 0) {

            return 0;
        }

        return aTimer.mServiceTimeSum_g / aTimer.mAccessCount_g;
    }

    /**
     * Returns the values of the MethodTimer in array. <p>
     *
     * @return an array of long[] of the method values
     */
    public long[] getValues() {

        long[] times = new long[8];

        times[TOTAL_ACCESS_COUNT] = mAccessCount_g;
        times[TOTAL_AVERAGE] = MethodTimer.average_g(this);
        times[TOTAL_MIN_TIME] = mServiceTimeMin_g;
        times[TOTAL_MAX_TIME] = mServiceTimeMax_g;
        times[CURRENT_ACCESS_COUNT] = mAccessCount;
        times[CURRENT_AVERAGE] = MethodTimer.average(this);

        if (mServiceTimeMin < 0) {
            times[CURRENT_MIN_TIME] = 0;
        } else {
            times[CURRENT_MIN_TIME] = mServiceTimeMin;
        }

        times[CURRENT_MAX_TIME] = mServiceTimeMax;

        reset();

        return times;
    }

    protected void reset() {
        // Reset the variables since last request
        mAccessCount = 0;
        mServiceTimeSum = 0;
        mServiceTimeSumOfSquares = 0;
        mServiceTimeMin = -1;
        mServiceTimeMax = 0;
    }

    /**
     * Used to sort the map of MethodTimers by name.
     */
    public int compareTo(Object aObject) {

        MethodTimer timer = (MethodTimer) aObject;

        return String.CASE_INSENSITIVE_ORDER.compare(this.getName(), timer.getName());
    }

    /**
     * Stop timing a method.
     */
    public void done() {

        // If the dummy timer, then leave
        if (mDummyTimer == this) {
            return;
        }

        // Check to see if done was called before
        if (!isOn() || mDoneFlag) {
            return;
        }

        Map classMap = null;

        synchronized (cPackages) {
            Map groupingMap = null;

            Object o = groupingThreadLocal.get();
            if (o != null) {
                groupingMap = (Map) cGroupings.get(o);

                if (groupingMap == null) {
                    groupingMap = new ConcurrentHashMap();
                    cGroupings.put(o, groupingMap);
                }
            } else {
                groupingMap = cPackages;
            }

            // Load and create the package specific Map if needed
            Map packageMap = (Map) groupingMap.get(mPackage);

            if (packageMap == null) {
                packageMap = new ConcurrentHashMap();
                groupingMap.put(mPackage, packageMap);
            }

            // Load and create the class specific Map if needed
            classMap = (Map) packageMap.get(mClass);

            if (classMap == null) {
                classMap = new ConcurrentHashMap();
                packageMap.put(mClass, classMap);
            }
        }

        mAccessCount = 1;
        mServiceTimeSum = System.currentTimeMillis() - mServiceTimeSum;
        mServiceTimeSumOfSquares = mServiceTimeSum * mServiceTimeSum;

        // see if the statistic is registered yet
        MethodTimer timer = (MethodTimer) classMap.get(this.mName);

        if (timer == null) {
            // register this as a new statistic name
            this.mServiceTimeMin = this.mServiceTimeSum;
            this.mServiceTimeMax = this.mServiceTimeSum;
            this.mServiceTimeMin_g = this.mServiceTimeSum;
            this.mServiceTimeMax_g = this.mServiceTimeSum;
            this.mAccessCount_g = 1;
            this.mServiceTimeSum_g = this.mServiceTimeSum;
            this.mServiceTimeSumOfSquares_g = this.mServiceTimeSumOfSquares;

            classMap.put(this.mName, this);
        } else {
            // add this statistic to accumulator
            timer.mAccessCount_g += this.mAccessCount;
            timer.mServiceTimeSum_g += this.mServiceTimeSum;
            timer.mServiceTimeSumOfSquares_g += this.mServiceTimeSumOfSquares;

            if (timer.mServiceTimeMin_g > this.mServiceTimeSum) {
                timer.mServiceTimeMin_g = this.mServiceTimeSum;
            }

            if (timer.mServiceTimeMax_g < this.mServiceTimeSum) {
                timer.mServiceTimeMax_g = this.mServiceTimeSum;
            }

            timer.mAccessCount += this.mAccessCount;
            timer.mServiceTimeSum += this.mServiceTimeSum;
            timer.mServiceTimeSumOfSquares += this.mServiceTimeSumOfSquares;

            if (timer.mServiceTimeMin > this.mServiceTimeSum || timer.mServiceTimeMin == -1) {
                timer.mServiceTimeMin = this.mServiceTimeSum;
            }

            if (timer.mServiceTimeMax < this.mServiceTimeSum) {
                timer.mServiceTimeMax = this.mServiceTimeSum;
            }
        }

        mDoneFlag = true;
    }

    /**
     * Sets the author of the timed code
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Gets the author for the timed code
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Sets the description of the timed code
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the description for the timed code
     */
    public void setDescription(String description) {
        this.description = description;
    }
}
