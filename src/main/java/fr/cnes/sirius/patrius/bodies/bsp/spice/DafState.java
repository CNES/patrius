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

/**
 * Define a DAF binary file state indicating where in the file we are 
 * and containing the last summary record read
 * @author T0281925
 *
 */
public class DafState {
    /**
     * File handle
     */
    private int handle;
    /**
     * Record number of previous array summary.
     */
    private int recnoPrevSummary;
    /**
     * Record number of current array summary.
     */
    private int recnoCurrSummary;
    /**
     * Record number of next array summary.
     */
    private int recnoNextSummary;
    /**
     * Number of summaries in current summary record.
     */
    private int nSummariesCurrSummaryRecord;
    /**
     * Index of current summary within summary record.
     */
    private int indexCurrSummary;
    /**
     * Last name record read.
     */
    private String lastNameRecord;
    /**
     * Flag indicating whether name record containing
     * name of current array is buffered.
     */
    private boolean buffered;
    /**
     * Last summary record read.
     */
    private double[] lastSummaryRecord;

    /**
     * Constructor
     */
    public DafState() {
        final int nWords = 128;
        handle = 0;
        recnoPrevSummary = 0;
        recnoCurrSummary = 0;
        recnoNextSummary = 0;
        nSummariesCurrSummaryRecord = 0;
        indexCurrSummary = 0;
        lastNameRecord = "";
        buffered = false;
        lastSummaryRecord = new double[nWords];
    }

    /**
     * Get state handle
     * @return handle of the DAF
     */
    public int getHandle() {
        return handle;
    }

    /**
     * Set State handle
     * @param handle of the DAF
     */
    public void setHandle(final int handle) {
        this.handle = handle;
    }
    
    /**
     * get the record containing the previous summary record
     * @return previous summary record
     */
    public int getRecnoPrevSummary() {
        return recnoPrevSummary;
    }

    /**
     * get the record containing the current summary record
     * @return current summary record
     */
    public int getRecnoCurrSummary() {
        return recnoCurrSummary;
    }

    /**
     * Set the current summary record of the DAF
     * @param recnoCurrSummary current summary record
     */
    public void setRecnoCurrSummary(final int recnoCurrSummary) {
        this.recnoCurrSummary = recnoCurrSummary;
    }

    /**
     * get the record containing the next summary record
     * @return next summary record
     */
    public int getRecnoNextSummary() {
        return recnoNextSummary;
    }

    /**
     * Get the number of summaries in the current summary record
     * @return number of summaries in the current summary record
     */
    public int getnSummariesCurrSummaryRecord() {
        return nSummariesCurrSummaryRecord;
    }

    /**
     * Get the index of the current summary within the summary record
     * @return index of the current summary within the summary record
     */
    public int getIndexCurrSummary() {
        return indexCurrSummary;
    }
    
    /**
     * Set the index of the current summary within the summary record
     * @param indexCurrSummary index of the current summary in the summary record
     */
    public void setIndexCurrSummary(final int indexCurrSummary) {
        this.indexCurrSummary = indexCurrSummary;
    }

    /**
     * Get the name of the last name record read
     * @return name of the last name record read
     */
    public String getLastNameRecord() {
        return lastNameRecord;
    }

    /**
     * Set the name of the last name record read
     * @param lastNameRecord last name record read
     */
    public void setLastNameRecord(final String lastNameRecord) {
        this.lastNameRecord = lastNameRecord;
    }

    /**
     * Get whether name record containing name of current array is buffered
     * @return boolean indicating if the name record is buffered
     */
    public boolean isBuffered() {
        return buffered;
    }

    /**
     * Set whether name record containing name of current array is buffered
     * @param isBuf if name record containing name of current array is buffered
     */
    public void setBuffered(final boolean isBuf) {
        this.buffered = isBuf;
    }

    /**
     * Get the contents of the last summary record
     * @return a double array containing the content of the last summary record
     */
    public double[] getLastSummaryRecord() {
        return Arrays.copyOf(lastSummaryRecord, lastSummaryRecord.length);
    }

    /**
     * Store the content of the last summary record into the state
     * @param lastSummaryRecord double array containing the last summary record content
     */
    public void setLastSummaryRecord(final double[] lastSummaryRecord) {
        this.lastSummaryRecord = Arrays.copyOf(lastSummaryRecord, lastSummaryRecord.length);
        // As we set the last summaryRecord, some others are updated automatically
        this.recnoNextSummary = (int) lastSummaryRecord[0];
        this.recnoPrevSummary = (int) lastSummaryRecord[1];
        this.nSummariesCurrSummaryRecord = (int) lastSummaryRecord[2];
    }

    /**
     * Calculates a unique numeric identifier for each DafState object
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + handle;
        result = prime * result + nSummariesCurrSummaryRecord;
        result = prime * result + recnoCurrSummary;
        result = prime * result + recnoNextSummary;
        result = prime * result + recnoPrevSummary;
        return result;
    }

    /**
     * Compare 2 DafStates to determine if they are the same
     * This is a must have to be able to create a list of DafState
     */
    @Override
    public boolean equals(final Object obj) {
        // Some fast verifications
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        // Create a DafState from the object
        final DafState other = (DafState) obj;
        // Compare the interesting attributes (those that are numeric)
        return (handle == other.handle) && (nSummariesCurrSummaryRecord == other.nSummariesCurrSummaryRecord) &&
                (recnoCurrSummary == other.recnoCurrSummary) && (recnoNextSummary == other.recnoNextSummary) &&
                (recnoPrevSummary == other.recnoPrevSummary);

    }

}
