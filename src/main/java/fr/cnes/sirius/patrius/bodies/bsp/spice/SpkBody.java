/**
 * Copyright 2023-2023 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.bsp.spice;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Class that allows to instantiate a SpkBody to be used in {@link SpkFile} . 
 * Each body contains all the segments corresponding to it that were read among 
 * other informations.
 * 
 * This class is based on the data structure description of SPKBSR in the 
 * SPICE library.
 * @author T0281925
 *
 */
public class SpkBody {

    /**
     * Size of the descriptor array
     */
    public static final int SIZEDESC = 5;
    /**
     * 
     */
    private List<SpkSegment> segmentTable;
    /**
     * Body id.
     */
    private final int body;
    /**
     * Expense at which the segments list was constructed. 
     * (The expense of a body list is the number of
     * segment descriptors examined during the construction of the list.)
     */
    private int expense;
    /**
     * highest file number searched during the construction of the segment list.
     * 
     */
    private int highestFile;
    /**
     * lowest file number searched during the construction of the segment list.
     */
    private int lowestFile;
    /**
     * Lower bound of the "re-use interval" of the previous segment returned.
     * 
     * The "re-use interval" is the maximal interval containing the epoch
     * of the last request for data for this body, such that the interval
     * is not masked by higher-priority segments.    
     */
    private double lowerBound;
    /**
     * Upper bound of the re-use interval of the previous segment returned 
     * 
     * The "re-use interval" is the maximal interval containing the epoch
     * of the last request for data for this body, such that the interval
     * is not masked by higher-priority segments.
     */
    private double upperBound;
    /**
     * Previous descriptor returned.
     */
    private double[] previousDescriptor;
    /**
     * Previous segment identifier returned.
     */
    private String previousSegmentId;
    /**
     * Previous handle returned.
     */
    private int previousHandle;
    /**
     * Logical indicating that previous segment should be checked to see whether it satisfies a request.
     */
    private boolean checkPrevious;
    /**
     * Expense of the re-use interval.
     */
    private int reuseExpense;

    /**
     * Constructor 
     * @param body body id
     * @param expense  Expense at which the segments list was constructed. 
     * @param highestFile highest file number searched during the construction of the segment list.
     * @param lowestFile lowest file number searched during the construction of the segment list.
     * @param lowerBound Lower bound of the re-use interval of the previous segment returned 
     * @param upperBound Upper bound of the re-use interval of the previous segment returned 
     * @param previousDescriptor Previous descriptor returned.
     * @param previousSegmentId Previous segment identifier returned.
     * @param previousHandle Previous handle returned.
     * @param checkPrevious Logical indicating that previous segment should be checked
     * @param reuseExpense Expense of the re-use interval.
     */
    public SpkBody(final int body,
            final int expense,
            final int highestFile,
            final int lowestFile,
            final double lowerBound,
            final double upperBound,
            final double[] previousDescriptor,
            final String previousSegmentId,
            final int previousHandle,
            final boolean checkPrevious,
            final int reuseExpense) {
        // Initialize the class instance
        segmentTable = new LinkedList<SpkSegment>();
        this.body = body;
        this.expense = expense;
        this.highestFile = highestFile;
        this.lowestFile = lowestFile;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.previousDescriptor = Arrays.copyOf(previousDescriptor,SIZEDESC);
        this.previousSegmentId = previousSegmentId;
        this.previousHandle = previousHandle;
        this.checkPrevious = checkPrevious;
        this.reuseExpense = reuseExpense;
    }
    
    /**
     * Simple constructor for finding SpkBody in a list.
     * @param body Body identifier
     */
    public SpkBody(final int body) {
        this.body = body;
        this.expense = 0;
        this.highestFile = 0;
        this.lowestFile = 0;
        this.lowerBound = 0.0;
        this.upperBound = 0.0;
        this.previousDescriptor = new double[SIZEDESC];
        this.previousSegmentId = "";
        this.previousHandle = -1;
        this.checkPrevious = false;
        this.reuseExpense = 0;
    }

    /**
     * Get the segment list
     * @return a list containing all the segments concerning the body
     */
    public List<SpkSegment> getSegmentTable() {
        return segmentTable;
    }

    /**
     * Get the expense
     * @return the expense associated to the body
     */
    public int getExpense() {
        return expense;
    }

    /**
     * Set the expense associated to a body
     * @param expense Expense to associate to the body
     */
    public void setExpense(final int expense) {
        this.expense = expense;
    }

    /**
     * Get the highest file number searched during the construction of the segment list
     * @return the highest file number searched during the construction of the segment list
     */
    public int getHighestFile() {
        return highestFile;
    }

    /**
     * Set the highest file number searched during the construction of the segment list
     * @param highestFile highest file number searched during the construction of the segment list
     */
    public void setHighestFile(final int highestFile) {
        this.highestFile = highestFile;
    }

    /**
     * Get the lowest file number searched during the construction of the segment list
     * @return the lowest file number searched during the construction of the segment list
     */
    public int getLowestFile() {
        return lowestFile;
    }

    /**
     * Set the lowest file number searched during the construction of the segment list
     * @param lowestFile the lowest file number searched during the construction of the segment list
     */
    public void setLowestFile(final int lowestFile) {
        this.lowestFile = lowestFile;
    }

    /**
     * Get the lower bound of the re-use interval
     * @return the lower bound of the re-use interval
     */
    public double getLowerBound() {
        return lowerBound;
    }

    /**
     * Set the lower bound of the re-use interval
     * @param lowerBound the lower bound of the re-use interval
     */
    public void setLowerBound(final double lowerBound) {
        this.lowerBound = lowerBound;
    }

    /**
     * Get the upper bound of the re-use interval
     * @return the upper bound of the re-use interval
     */
    public double getUpperBound() {
        return upperBound;
    }

    /**
     * Set the upper bound of the re-use interval
     * @param upperBound the upper bound of the re-use interval
     */
    public void setUpperBound(final double upperBound) {
        this.upperBound = upperBound;
    }

    /**
     * Get the previous descriptor returned.
     * @return Previous descriptor returned.
     */
    public double[] getPreviousDescriptor() {
        return Arrays.copyOf(previousDescriptor, SIZEDESC);
    }

    /**
     * Set the previous descriptor returned.
     * @param previousDescriptor Previous descriptor returned.
     */
    public void setPreviousDescriptor(final double[] previousDescriptor) {
        this.previousDescriptor = Arrays.copyOf(previousDescriptor, SIZEDESC);
    }

    /**
     * Get the previous segment identifier returned.
     * @return the previous segment identifier returned.
     */
    public String getPreviousSegmentId() {
        return previousSegmentId;
    }

    /**
     * Set the previous segment identifier returned.
     * @param previousSegmentId the previous segment identifier returned.
     */
    public void setPreviousSegmentId(final String previousSegmentId) {
        this.previousSegmentId = previousSegmentId;
    }

    /**
     * Get the previous handle returned.
     * @return the previous handle returned.
     */
    public int getPreviousHandle() {
        return previousHandle;
    }

    /**
     * Set the previous handle returned.
     * @param previousHandle the previous handle returned.
     */
    public void setPreviousHandle(final int previousHandle) {
        this.previousHandle = previousHandle;
    }

    /**
     * Get if the previous segment needs to be check
     * @return a boolean indicating if the previous segment needs to be checked
     */
    public boolean isCheckPrevious() {
        return checkPrevious;
    }

    /**
     * Set the boolean indicating if the previous segment needs to be checked
     * @param checkPrevious boolean indicating if the previous segment needs to be checked
     */
    public void setCheckPrevious(final boolean checkPrevious) {
        this.checkPrevious = checkPrevious;
    }

    /**
     * Get the expense of the re-use interval.
     * @return the expense of the re-use interval.
     */
    public int getReuseExpense() {
        return reuseExpense;
    }

    /**
     * Set the expense of the re-use interval.
     * @param reuseExpense the expense of the re-use interval.
     */
    public void setReuseExpense(final int reuseExpense) {
        this.reuseExpense = reuseExpense;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + body;
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        // Check if both objects have the same reference
        if (this == obj) {
            return true;
        }
        // If the object has no reference
        if (obj == null) {
            return false;
        }
        // If the object is not of the type SpkBody
        if (!(obj instanceof SpkBody)) {
            return false;
        }
        // Instanciate a SpkBody
        final SpkBody other = (SpkBody) obj;
        return body == other.body;
    }
    
    

}
