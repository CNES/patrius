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
 * VERSION:4.11:DM:DM-3218:22/05/2023:[PATRIUS] Evolution de format des fichiers SP3
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
/*
 * 
 */
package fr.cnes.sirius.patrius.files.general;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeStamped;

/**
 * Contains the position/velocity of a satellite at an specific epoch.
 * 
 * @author Thomas Neidhart
 */
public class SatelliteTimeCoordinate implements TimeStamped, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -2099947583052252633L;

    /** Epoch for this entry. */
    private AbsoluteDate epoch;

    /**
     * Position/velocity coordinates for this entry in SI units (position in meters, velocity in meters/second and
     * acceleration in meters/(second^2)).
     */
    private PVCoordinates coordinate;

    /** Clock correction in microseconds. */
    private double clockCorrection;

    /** Clock rate change in 10^(-4) microseconds/second. */
    private double clockRateChange;

    /**
     * Creates a new {@link SatelliteTimeCoordinate} instance with
     * a given epoch and coordinate.
     * 
     * @param time
     *        the epoch of the entry
     * @param coord
     *        the coordinate of the entry in SI units (position in meters, velocity in meters/second and acceleration in
     *        meters/(second^2))
     */
    public SatelliteTimeCoordinate(final AbsoluteDate time,
        final PVCoordinates coord) {
        this(time, coord, 0.0d, 0.0d);
    }

    /**
     * Creates a new {@link SatelliteTimeCoordinate} object with a given epoch
     * and position coordinate. The velocity is set to a zero vector.
     * 
     * @param time
     *        the epoch of the entry
     * @param pos
     *        the position coordinate of the entry in meters
     * @param clock
     *        the clock value in microseconds
     */
    public SatelliteTimeCoordinate(final AbsoluteDate time,
        final Vector3D pos, final double clock) {
        this(time, new PVCoordinates(pos, Vector3D.ZERO), clock, 0.0d);
    }

    /**
     * Creates a new {@link SatelliteTimeCoordinate} instance with a given
     * epoch, coordinate and clock value / rate change.
     * 
     * @param time
     *        the epoch of the entry
     * @param coord
     *        the coordinate of the entry in SI units (position in meters, velocity in meters/second and acceleration in
     *        meters/(second^2))
     * @param clockCorr
     *        the clock value that corresponds to this coordinate in microseconds
     * @param rateChange
     *        the clock rate change in 10^(-4) microseconds/second
     */
    public SatelliteTimeCoordinate(final AbsoluteDate time,
        final PVCoordinates coord,
        final double clockCorr,
        final double rateChange) {
        this.epoch = time;
        this.coordinate = coord;
        this.clockCorrection = clockCorr;
        this.clockRateChange = rateChange;
    }

    /**
     * Returns the epoch for this coordinate.
     * 
     * @return the epoch
     */
    public AbsoluteDate getEpoch() {
        return this.epoch;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return this.getEpoch();
    }

    /**
     * Set the epoch for this coordinate.
     * 
     * @param epochIn
     *        the epoch to be set
     */
    public void setEpoch(final AbsoluteDate epochIn) {
        this.epoch = epochIn;
    }

    /**
     * Returns the coordinate of this entry.
     * 
     * @return the coordinate in SI units (position in meters, velocity in meters/second and acceleration in
     *         meters/(second^2))
     */
    public PVCoordinates getCoordinate() {
        return this.coordinate;
    }

    /**
     * Set the coordinate for this entry.
     * 
     * @param coordinateIn
     *        the coordinate to be set in SI units (position in meters, velocity in meters/second and acceleration in
     *        meters/(second^2))
     */
    public void setCoordinate(final PVCoordinates coordinateIn) {
        this.coordinate = coordinateIn;
    }

    /**
     * Returns the clock correction value.
     * 
     * @return the clock correction in microseconds
     */
    public double getClockCorrection() {
        return this.clockCorrection;
    }

    /**
     * Set the clock correction to the given value.
     * 
     * @param corr
     *        the clock correction value in microseconds
     */
    public void setClockCorrection(final double corr) {
        this.clockCorrection = corr;
    }

    /**
     * Returns the clock rate change value.
     * 
     * @return the clock rate change in 10^(-4) microseconds/second
     */
    public double getClockRateChange() {
        return this.clockRateChange;
    }

    /**
     * Set the clock rate change to the given value.
     * 
     * @param rateChange
     *        the clock rate change value in 10^(-4) microseconds/second
     */
    public void setClockRateChange(final double rateChange) {
        this.clockRateChange = rateChange;
    }
}
