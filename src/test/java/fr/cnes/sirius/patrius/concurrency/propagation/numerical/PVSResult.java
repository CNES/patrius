/**
 * 
 * Copyright 2011-2022 CNES
 *
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
 *
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 *  */
package fr.cnes.sirius.patrius.concurrency.propagation.numerical;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.tools.parallel.ParallelResult;
import fr.cnes.sirius.patrius.tools.parallel.ParallelTask;

/**
 * Results, as a PVCoordinates list.
 * 
 * @author cardosop
 * 
 * @version $Id: PVSResult.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.2
 * 
 */
public class PVSResult implements ParallelResult {

    /** Position comparison threshold */
    private static final double POSITION_THRESHOLD = 1e-4;

    /** Angle allowed between two position vectors (arcsecond) */
    private static final double ANGULAR_DEVIATION_THRESHOLD = 1e-4;

    /** Velocity comparison threshold */
    private static final double VELOCITY_THRESHOLD = 1e-7;

    /** Maximum position error. */
    private double maxPositionError;
    /** Maximum velocity error. */
    private double maxVelocityError;
    /** Maximum angle deviation. */
    private double maxAngleDeviation;

    /** List of PVCoordinates. */
    private final List<PVCoordinates> pvList = new ArrayList<PVCoordinates>();

    /** Parent task */
    private final ParallelTask parent;

    /**
     * Adds a new PV to the list.
     * 
     * @param newPV
     *        the new PV.
     */
    public void addPV(final PVCoordinates newPV) {
        this.pvList.add(newPV);
    }

    /**
     * Constructor.
     * 
     * @param task
     *        : task
     */
    public PVSResult(final ParallelTask task) {
        this.parent = task;
        this.maxPositionError = 0.;
        this.maxVelocityError = 0.;
        this.maxAngleDeviation = 0.;
    }

    @Override
    public double[][] getDataAsArray() {
        final double[][] rez = new double[this.pvList.size()][6];
        int counter = 0;
        for (final PVCoordinates pv : this.pvList) {
            final double[] pvar = new double[6];
            pvar[0] = pv.getPosition().getX();
            pvar[1] = pv.getPosition().getY();
            pvar[2] = pv.getPosition().getZ();
            pvar[3] = pv.getVelocity().getX();
            pvar[4] = pv.getVelocity().getY();
            pvar[5] = pv.getVelocity().getZ();
            rez[counter] = pvar;
            counter++;
        }
        return rez;
    }

    @Override
    public boolean resultEquals(final ParallelResult other) {
        boolean rez = true;
        if (other == null) {
            return false;
        }
        if (other instanceof PVSResult) {
            if (((PVSResult) other).pvList.size() == this.pvList.size()) {
                final PVCoordinates[] otherAR = ((PVSResult) other).pvList
                    .toArray(new PVCoordinates[0]);
                final PVCoordinates[] thisAR = this.pvList
                    .toArray(new PVCoordinates[0]);
                for (int i = 0; i < this.pvList.size(); i++) {

                    /*
                     * Position & velocity deviation
                     */
                    final double positionDevianceNorm = thisAR[i].getPosition()
                        .distance(otherAR[i].getPosition());
                    final double velocityDevianceNorm = thisAR[i].getVelocity()
                        .distance(otherAR[i].getVelocity());

                    /*
                     * Angle deviation between the two positions (in arc second)
                     */
                    final double positionDevianceAngle = Vector3D.angle(thisAR[i].getPosition(),
                        otherAR[i].getPosition());

                    final double angleInArcSecond = MathLib.toDegrees(positionDevianceAngle) / 3600.;

                    /*
                     * Checks & store the current max deviations.
                     */
                    if (positionDevianceNorm > this.maxPositionError) {
                        this.maxPositionError = positionDevianceNorm;
                    }
                    if (velocityDevianceNorm > this.maxVelocityError) {
                        this.maxVelocityError = velocityDevianceNorm;
                    }
                    if (angleInArcSecond > this.maxAngleDeviation) {
                        this.maxAngleDeviation = angleInArcSecond;
                    }

                    /*
                     * Checks if it pass the error threshold
                     */
                    if (positionDevianceNorm > POSITION_THRESHOLD) {
                        System.err.println("Position deviance is "
                            + positionDevianceNorm + " for line " + i);
                        rez = false;
                    }

                    if (angleInArcSecond > ANGULAR_DEVIATION_THRESHOLD) {
                        System.err.println("Angle between two position vectors is "
                            + angleInArcSecond + " for line " + i);
                        rez = false;
                    }

                    if (velocityDevianceNorm > VELOCITY_THRESHOLD) {
                        System.err.println("Velocity deviance is "
                            + velocityDevianceNorm + " for line " + i);
                        rez = false;
                    }
                }
            }
        }

        if (!rez) {
            System.err.println("** Errors in " + this.parent.getTaskInfo() + " **");
            System.err.println("Max position deviation : " + this.maxPositionError + " m");
            System.err.println("Max velocity deviation : " + this.maxVelocityError + " m/s");
            System.err.println("Max angle deviation : " + this.maxAngleDeviation + " arc second");

        } else if (this.maxAngleDeviation != 0. || this.maxPositionError != 0. ||
            this.maxVelocityError != 0.) {
            System.out.println("/!\\ Warning in " + this.parent.getTaskInfo() + " : ");
            System.out.println("Max position deviation : " + this.maxPositionError + " m");
            System.out.println("Max velocity deviation : " + this.maxVelocityError + " m/s");
            System.out.println("Max angle deviation : " + this.maxAngleDeviation + " arc second");

        }

        return rez;
    }

}
