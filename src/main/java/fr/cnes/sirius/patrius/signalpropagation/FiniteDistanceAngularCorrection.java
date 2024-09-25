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
 * HISTORY
 * VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation;

import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This interface extends the {@link AngularCorrection} to take into account an angular correction when the distance
 * between the observer and the target is not infinite (i.e.: the parallax correction).
 *
 * @author veuillh
 * 
 * @since 4.13
 */
public interface FiniteDistanceAngularCorrection extends AngularCorrection {

    /**
     * Compute the angular correction from the apparent elevation and distance.
     *
     * <p>
     * This method takes into account the finite distance of the observed object to add a parallax correction.
     * </p>
     *
     * @param date
     *        The date at which we want to compute the angular correction
     * @param apparentElevation
     *        The apparent elevation (with atmosphere) [rad]
     * @param distance
     *        The distance to the object [m]. Can be {@link Double#POSITIVE_INFINITY} (equivalent to not take into
     *        account the parallax correction)
     * @return the elevation correction [rad] so that
     *         {@code apparent_elevation = geometric_elevation + elevation_correction}
     */
    double computeElevationCorrectionFromApparentElevation(final AbsoluteDate date, final double apparentElevation,
                                                           double distance);

    /**
     * Compute the angular correction from the geometric elevation and distance.
     *
     * <p>
     * This method takes into account the finite distance of the observed object to add a parallax correction.
     * </p>
     *
     * @param date
     *        The date at which we want to compute the angular correction
     * @param geometricElevation
     *        The geometric elevation (without atmosphere) [rad]
     * @param distance
     *        The distance to the object [m]. Can be {@link Double#POSITIVE_INFINITY} (equivalent to not take into
     *        account the parallax correction)
     * @return the elevation correction [rad] so that
     *         {@code apparent_elevation = geometric_elevation + elevation_correction}
     */
    double computeElevationCorrectionFromGeometricElevation(final AbsoluteDate date, final double geometricElevation,
                                                            double distance);

    /** {@inheritDoc} */
    @Override
    default double computeElevationCorrectionFromApparentElevation(final AbsoluteDate date,
                                                                   final double apparentElevation) {
        return computeElevationCorrectionFromApparentElevation(date, apparentElevation, Double.POSITIVE_INFINITY);
    }

    /** {@inheritDoc} */
    @Override
    default double computeElevationCorrectionFromGeometricElevation(final AbsoluteDate date,
                                                                    final double geometricElevation) {
        return computeElevationCorrectionFromGeometricElevation(date, geometricElevation, Double.POSITIVE_INFINITY);
    }
}
