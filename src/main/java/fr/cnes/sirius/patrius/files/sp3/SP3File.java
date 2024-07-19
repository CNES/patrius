/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2022 CNES
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
/*
 * 
 */
package fr.cnes.sirius.patrius.files.sp3;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import fr.cnes.sirius.patrius.files.general.OrbitFile;
import fr.cnes.sirius.patrius.files.general.SatelliteInformation;
import fr.cnes.sirius.patrius.files.general.SatelliteTimeCoordinate;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * Represents a parsed SP3 orbit file.
 * 
 * @author Thomas Neidhart
 */
public class SP3File implements OrbitFile, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 3333652174843017654L;

    /** File type indicator. */
    public enum SP3FileType {
        /** GPS only file. */
        GPS,
        /** Mixed file. */
        MIXED,
        /** GLONASS only file. */
        GLONASS,
        /** LEO only file. */
        LEO,
        /** Galileo only file. */
        GALILEO,
        /** COMPASS only file. */
        COMPASS,
        /** QZSS only file. */
        QZSS,
        /** undefined file format. */
        UNDEFINED
    }

    /** Orbit type indicator. */
    public enum SP3OrbitType {
        /** fitted. */
        FIT,
        /** extrapolated or predicted. */
        EXT,
        /** broadcast. */
        BCT,
        /** fitted after applying a Helmert transformation. */
        HLM
    }

    /** File type. */
    private SP3FileType type;

    /** Time system. */
    private TimeSystem timeSystem;

    /** Epoch of the file. */
    private AbsoluteDate epoch;

    /** GPS week. */
    private int gpsWeek;

    /** Seconds of the current GPS week. */
    private double secondsOfWeek;

    /** Julian day. */
    private int julianDay;

    /** Day fraction. */
    private double dayFraction;

    /** Time-interval between epochs. */
    private double epochInterval;

    /** Number of epochs. */
    private int numberOfEpochs;

    /** Coordinate system. */
    private String coordinateSystem;

    /** Data used indicator. */
    private String dataUsed;

    /** Orbit type. */
    private SP3OrbitType orbitType;

    /** Agency providing the file. */
    private String agency;

    /** A list containing additional satellite information. */
    private final List<SatelliteInformation> satellites;

    /** A mapping of satellite id to its corresponding {@link SatelliteInformation} object. */
    private final Map<String, SatelliteInformation> satelliteInfo;

    /** A map containing all satellite coordinates. */
    private final Map<String, List<SatelliteTimeCoordinate>> satelliteCoords;

    /** Create a new SP3 file object. */
    public SP3File() {
        this.satellites = new ArrayList<>();
        this.satelliteInfo = new HashMap<>();
        this.satelliteCoords = new HashMap<>();
    }

    /**
     * Returns the {@link SP3FileType} associated with this SP3 file.
     * 
     * @return the file type for this SP3 file
     */
    public SP3FileType getType() {
        return this.type;
    }

    /**
     * Set the file type for this SP3 file.
     * 
     * @param fileType
     *        the file type to be set
     */
    public void setType(final SP3FileType fileType) {
        this.type = fileType;
    }

    /** {@inheritDoc} */
    @Override
    public TimeSystem getTimeSystem() {
        return this.timeSystem;
    }

    /**
     * Set the time system used in this SP3 file.
     * 
     * @param system
     *        the time system to be set
     */
    public void setTimeSystem(final TimeSystem system) {
        this.timeSystem = system;
    }

    /**
     * Returns the data used indicator from the SP3 file.
     * 
     * @return the data used indicator (unparsed)
     */
    public String getDataUsed() {
        return this.dataUsed;
    }

    /**
     * Set the data used indicator for this SP3 file.
     * 
     * @param data
     *        the data used indicator to be set
     */
    public void setDataUsed(final String data) {
        this.dataUsed = data;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getEpoch() {
        return this.epoch;
    }

    /**
     * Set the epoch of the SP3 file.
     * 
     * @param time
     *        the epoch to be set
     */
    public void setEpoch(final AbsoluteDate time) {
        this.epoch = time;
    }

    /**
     * Returns the GPS week as contained in the SP3 file.
     * 
     * @return the GPS week of the SP3 file
     */
    public int getGpsWeek() {
        return this.gpsWeek;
    }

    /**
     * Set the GPS week of the SP3 file.
     * 
     * @param week
     *        the GPS week to be set
     */
    public void setGpsWeek(final int week) {
        this.gpsWeek = week;
    }

    /**
     * Returns the seconds of the GPS week as contained in the SP3 file.
     * 
     * @return the seconds of the GPS week
     */
    public double getSecondsOfWeek() {
        return this.secondsOfWeek;
    }

    /**
     * Set the seconds of the GPS week for this SP3 file.
     * 
     * @param seconds
     *        the seconds to be set
     */
    public void setSecondsOfWeek(final double seconds) {
        this.secondsOfWeek = seconds;
    }

    /**
     * Returns the julian day for this SP3 file.
     * 
     * @return the julian day
     */
    public int getJulianDay() {
        return this.julianDay;
    }

    /**
     * Set the julian day for this SP3 file.
     * 
     * @param day
     *        the julian day to be set
     */
    public void setJulianDay(final int day) {
        this.julianDay = day;
    }

    /**
     * Returns the day fraction for this SP3 file.
     * 
     * @return the day fraction
     */
    public double getDayFraction() {
        return this.dayFraction;
    }

    /**
     * Set the day fraction for this SP3 file.
     * 
     * @param fraction
     *        the day fraction to be set
     */
    public void setDayFraction(final double fraction) {
        this.dayFraction = fraction;
    }

    /** {@inheritDoc} */
    @Override
    public double getEpochInterval() {
        return this.epochInterval;
    }

    /**
     * Set the epoch interval for this SP3 file.
     * 
     * @param interval
     *        the interval between orbit entries
     */
    public void setEpochInterval(final double interval) {
        this.epochInterval = interval;
    }

    /** {@inheritDoc} */
    @Override
    public int getNumberOfEpochs() {
        return this.numberOfEpochs;
    }

    /**
     * Set the number of epochs as contained in the SP3 file.
     * 
     * @param epochCount
     *        the number of epochs to be set
     */
    public void setNumberOfEpochs(final int epochCount) {
        this.numberOfEpochs = epochCount;
    }

    /** {@inheritDoc} */
    @Override
    public String getCoordinateSystem() {
        return this.coordinateSystem;
    }

    /**
     * Set the coordinate system used for the orbit entries.
     * 
     * @param system
     *        the coordinate system to be set
     */
    public void setCoordinateSystem(final String system) {
        this.coordinateSystem = system;
    }

    /**
     * Returns the {@link SP3OrbitType} for this SP3 file.
     * 
     * @return the orbit type
     */
    public SP3OrbitType getOrbitType() {
        return this.orbitType;
    }

    /**
     * Set the {@link SP3OrbitType} for this SP3 file.
     * 
     * @param oType
     *        the orbit type to be set
     */
    public void setOrbitType(final SP3OrbitType oType) {
        this.orbitType = oType;
    }

    /**
     * Returns the agency that prepared this SP3 file.
     * 
     * @return the agency
     */
    public String getAgency() {
        return this.agency;
    }

    /**
     * Set the agency string for this SP3 file.
     * 
     * @param agencyStr
     *        the agency string to be set
     */
    public void setAgency(final String agencyStr) {
        this.agency = agencyStr;
    }

    /**
     * Add a new satellite with a given identifier to the list of
     * stored satellites.
     * 
     * @param satId
     *        the satellite identifier
     */
    public void addSatellite(final String satId) {
        // only add satellites which have not been added before
        if (this.getSatellite(satId) == null) {
            final SatelliteInformation info = new SatelliteInformation(satId);
            this.satellites.add(info);
            this.satelliteInfo.put(satId, info);
            this.satelliteCoords.put(satId, new LinkedList<SatelliteTimeCoordinate>());
        }
    }

    /** {@inheritDoc} */
    @Override
    public Collection<SatelliteInformation> getSatellites() {
        return Collections.unmodifiableCollection(this.satellites);
    }

    /** {@inheritDoc} */
    @Override
    public int getSatelliteCount() {
        return this.satellites.size();
    }

    /** {@inheritDoc} */
    @Override
    public SatelliteInformation getSatellite(final String satId) {
        if (satId == null) {
            return null;
        }

        return this.satelliteInfo.get(satId);
    }

    /**
     * Returns the nth satellite as contained in the SP3 file.
     * 
     * @param n
     *        the index of the satellite
     * @return a {@link SatelliteInformation} object for the nth satellite
     */
    public SatelliteInformation getSatellite(final int n) {
        return this.satellites.get(n);
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsSatellite(final String satId) {
        return this.satelliteCoords.containsKey(satId);
    }

    /** {@inheritDoc} */
    @Override
    public List<SatelliteTimeCoordinate> getSatelliteCoordinates(final String satId) {
        return this.satelliteCoords.get(satId);
    }

    /**
     * Adds a new P/V coordinate for a given satellite.
     * 
     * @param satId
     *        the satellite identifier
     * @param coord
     *        the P/V coordinate of the satellite
     */
    public void addSatelliteCoordinate(final String satId, final SatelliteTimeCoordinate coord) {
        final List<SatelliteTimeCoordinate> coords = this.satelliteCoords.get(satId);
        if (coords != null) {
            coords.add(coord);
        }
    }
}
