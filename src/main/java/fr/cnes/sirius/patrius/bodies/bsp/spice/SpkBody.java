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
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.bsp.spice;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Class that allows to instantiate a SpkBody to be used in {@link SpkFile}.<br>
 * Each body contains all the segments corresponding to it that were read among other informations.
 * <p>
 * This class is based on the data structure description of SPKBSR in the SPICE library.
 * </p>
 * 
 * @author T0281925
 *
 * @since 4.11
 */
public class SpkBody {

    /** Size of the descriptor array. */
    public static final int SIZEDESC = 5;
    /** Segments table. */
    private List<SpkSegment> segmentTable;

    /** Body id. */
    private final int body;

    /**
     * Expense at which the segments list was constructed.<br>
     * The expense of a body list is the number of segment descriptors examined during the construction of the list.
     */
    private int expense;

    /** Highest file number searched during the construction of the segment list. */
    private int highestFile;

    /** Lowest file number searched during the construction of the segment list. */
    private int lowestFile;

    /**
     * Lower bound of the "re-use interval" of the previous segment returned.<br>
     * The "re-use interval" is the maximal interval containing the epoch of the last request for data for this body,
     * such that the interval is not masked by higher-priority segments.
     */
    private double lowerBound;

    /**
     * Upper bound of the re-use interval of the previous segment returned.<br>
     * The "re-use interval" is the maximal interval containing the epochcof the last request for data for this body,
     * such that the interval is not masked by higher-priority segments.
     */
    private double upperBound;

    /** Previous descriptor returned. */
    private double[] previousDescriptor;

    /** Previous segment identifier returned. */
    private String previousSegmentId;

    /** Previous handle returned. */
    private int previousHandle;

    /** Logical indicating that previous segment should be checked to see whether it satisfies a request. */
    private boolean checkPrevious;

    /** Expense of the re-use interval. */
    private int reuseExpense;

    /**
     * Constructor.
     * 
     * @param body
     *        body id
     * @param expense
     *        Expense at which the segments list was constructed.
     * @param highestFile
     *        highest file number searched during the construction of the segment list
     * @param lowestFile
     *        lowest file number searched during the construction of the segment list
     * @param lowerBound
     *        Lower bound of the re-use interval of the previous segment returned
     * @param upperBound
     *        Upper bound of the re-use interval of the previous segment returned
     * @param previousDescriptor
     *        Previous descriptor returned
     * @param previousSegmentId
     *        Previous segment identifier returned
     * @param previousHandle
     *        Previous handle returned
     * @param checkPrevious
     *        Logical indicating that previous segment should be checked
     * @param reuseExpense
     *        Expense of the re-use interval
     */
    public SpkBody(final int body, final int expense, final int highestFile, final int lowestFile,
                   final double lowerBound, final double upperBound, final double[] previousDescriptor,
                   final String previousSegmentId, final int previousHandle, final boolean checkPrevious,
                   final int reuseExpense) {
        // Initialize the class instance
        this.segmentTable = new LinkedList<>();
        this.body = body;
        this.expense = expense;
        this.highestFile = highestFile;
        this.lowestFile = lowestFile;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.previousDescriptor = Arrays.copyOf(previousDescriptor, SIZEDESC);
        this.previousSegmentId = previousSegmentId;
        this.previousHandle = previousHandle;
        this.checkPrevious = checkPrevious;
        this.reuseExpense = reuseExpense;
    }

    /**
     * Simple constructor for finding SpkBody in a list.
     * 
     * @param body
     *        Body identifier
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
     * Getter for the segment list.
     * 
     * @return a list containing all the segments concerning the body
     */
    public List<SpkSegment> getSegmentTable() {
        return this.segmentTable;
    }

    /**
     * Getter for the expense.
     * 
     * @return the expense associated to the body
     */
    public int getExpense() {
        return this.expense;
    }

    /**
     * Setter for the expense associated to a body.
     * 
     * @param expense
     *        Expense to associate to the body
     */
    public void setExpense(final int expense) {
        this.expense = expense;
    }

    /**
     * Getter for the highest file number searched during the construction of the segment list.
     * 
     * @return the highest file number searched during the construction of the segment list
     */
    public int getHighestFile() {
        return this.highestFile;
    }

    /**
     * Setter for the highest file number searched during the construction of the segment list.
     * 
     * @param highestFile
     *        highest file number searched during the construction of the segment list
     */
    public void setHighestFile(final int highestFile) {
        this.highestFile = highestFile;
    }

    /**
     * Getter for the lowest file number searched during the construction of the segment list.
     * 
     * @return the lowest file number searched during the construction of the segment list
     */
    public int getLowestFile() {
        return this.lowestFile;
    }

    /**
     * Setter for the lowest file number searched during the construction of the segment list.
     * 
     * @param lowestFile
     *        the lowest file number searched during the construction of the segment list
     */
    public void setLowestFile(final int lowestFile) {
        this.lowestFile = lowestFile;
    }

    /**
     * Getter for the lower bound of the re-use interval.
     * 
     * @return the lower bound of the re-use interval
     */
    public double getLowerBound() {
        return this.lowerBound;
    }

    /**
     * Setter for the lower bound of the re-use interval.
     * 
     * @param lowerBound
     *        the lower bound of the re-use interval
     */
    public void setLowerBound(final double lowerBound) {
        this.lowerBound = lowerBound;
    }

    /**
     * Getter for the upper bound of the re-use interval.
     * 
     * @return the upper bound of the re-use interval
     */
    public double getUpperBound() {
        return this.upperBound;
    }

    /**
     * Setter for the upper bound of the re-use interval.
     * 
     * @param upperBound
     *        the upper bound of the re-use interval
     */
    public void setUpperBound(final double upperBound) {
        this.upperBound = upperBound;
    }

    /**
     * Getter for the previous descriptor returned.
     * 
     * @return Previous descriptor returned.
     */
    public double[] getPreviousDescriptor() {
        return Arrays.copyOf(this.previousDescriptor, SIZEDESC);
    }

    /**
     * Setter for the previous descriptor returned.
     * 
     * @param previousDescriptor
     *        Previous descriptor returned.
     */
    public void setPreviousDescriptor(final double[] previousDescriptor) {
        this.previousDescriptor = Arrays.copyOf(previousDescriptor, SIZEDESC);
    }

    /**
     * Getter for the previous segment identifier returned.
     * 
     * @return the previous segment identifier returned
     */
    public String getPreviousSegmentId() {
        return this.previousSegmentId;
    }

    /**
     * Setter for the previous segment identifier returned.
     * 
     * @param previousSegmentId
     *        the previous segment identifier returned
     */
    public void setPreviousSegmentId(final String previousSegmentId) {
        this.previousSegmentId = previousSegmentId;
    }

    /**
     * Getter for the previous handle returned.
     * 
     * @return the previous handle returned
     */
    public int getPreviousHandle() {
        return this.previousHandle;
    }

    /**
     * Setter for the previous handle returned.
     * 
     * @param previousHandle
     *        the previous handle returned
     */
    public void setPreviousHandle(final int previousHandle) {
        this.previousHandle = previousHandle;
    }

    /**
     * Indicates if the previous segment needs to be check.
     * 
     * @return a boolean indicating if the previous segment needs to be checked
     */
    public boolean isCheckPrevious() {
        return this.checkPrevious;
    }

    /**
     * Setter for the boolean indicating if the previous segment needs to be checked.
     * 
     * @param checkPrevious
     *        boolean indicating if the previous segment needs to be checked
     */
    public void setCheckPrevious(final boolean checkPrevious) {
        this.checkPrevious = checkPrevious;
    }

    /**
     * Getter for the expense of the re-use interval.
     * 
     * @return the expense of the re-use interval
     */
    public int getReuseExpense() {
        return this.reuseExpense;
    }

    /**
     * Setter for the expense of the re-use interval.
     * 
     * @param reuseExpense
     *        the expense of the re-use interval
     */
    public void setReuseExpense(final int reuseExpense) {
        this.reuseExpense = reuseExpense;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(this.body);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        // Check the object could be a Counter array
        boolean isEqual = false;

        if (obj == this) {
            // Identity
            isEqual = true;
        } else if ((obj != null) && (obj.getClass() == this.getClass())) {
            final SpkBody other = (SpkBody) obj;
            isEqual = Objects.equals(this.body, other.body);
        }

        return isEqual;
    }
}
