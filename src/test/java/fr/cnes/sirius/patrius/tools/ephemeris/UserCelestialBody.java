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
 * @history created 09/10/12
 *
 * HISTORY
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3149:10/05/2022:[PATRIUS] Optimisation des reperes interplanetaires 
 * VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
 * VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.tools.ephemeris;

import java.util.Arrays;

import fr.cnes.sirius.patrius.bodies.AbstractCelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyEphemeris;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunctionLagrangeForm;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * User-made Celestial body.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: UserCelestialBody.java 17578 2017-05-10 12:20:20Z bignon $
 * 
 * @since 1.2
 * 
 */
public class UserCelestialBody extends AbstractCelestialBody {

    /** Serializable UID. */
    private static final long serialVersionUID = -1863617019515445599L;

    /** Error string. */
    private static final String DATE_NOT_IN_MAP_INTERPOLATION_NOT_POSSIBLE =
        "The date is not in the map and linear interpolation is not possible";

    /** Ephemeris. */
    private final IUserEphemeris eph;

    /** The list of dates. */
    private final AbsoluteDate[] dateEphemeris;

    /** Frame configuration. */
    private FramesConfiguration framesConfiguration;

    /**
     * Constructor.
     * 
     * @param body
     *        the body
     * @param ephemeris
     *        the ephemeris
     */
    public UserCelestialBody(final IEphemerisBody body, final IUserEphemeris ephemeris) {
        super(body.name(), body.getGM(), body.getIAUPole(), ephemeris.getReferenceFrame());
        this.eph = ephemeris;
        this.dateEphemeris = this.eph.getEphemeris().keySet().toArray(new AbsoluteDate[0]);
        Arrays.sort(this.dateEphemeris);
        // Configuration is default one
        this.framesConfiguration = FramesFactory.getConfiguration();
        setEphemeris(new CelestialBodyEphemeris() {
            
            /** Serializable UID. */
            private static final long serialVersionUID = -7250280024229122819L;

            /** {@inheritDoc} */
            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date,
                    final Frame frame) throws PatriusException {
                return UserCelestialBody.this.getPVCoordinates(date, frame);
            }
            
            /** {@inheritDoc} */
            @Override
            public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
                return UserCelestialBody.this.getNativeFrame(date);
            }
        });
    }

    /**
     * Constructor with frame configuration.
     * 
     * @param body
     *        the body
     * @param ephemeris
     *        the ephemeris
     * @param framesConfigurationIn
     *        the frame configuration
     */
    public UserCelestialBody(final IEphemerisBody body, final IUserEphemeris ephemeris,
        final FramesConfiguration framesConfigurationIn) {
        this(body, ephemeris);
        this.framesConfiguration = framesConfigurationIn;
    }

    /**
     * Computes the PV coordinates at a date using Lagrange 4 interpolator.
     * 
     * @param date
     *        the date
     * @param frame
     *        the frame
     * @return the PV coordinates at the given date
     * @throws PatriusException
     *         should not happen
     */
    public PVCoordinates getPVCoordinatesLagrange4(final AbsoluteDate date, final Frame frame) throws PatriusException {

        if (this.eph.getEphemeris().containsKey(date)) {
            // no interpolation : the date is in the map
            final Transform t = this.eph.getReferenceFrame().getTransformTo(frame, date, this.framesConfiguration);
            return t.transformPVCoordinates(this.eph.getEphemeris().get(date));
        }

        // interpolation : the date is not in the map
        final int interpOrder = 8;

        if (this.eph.getEphemeris().size() > interpOrder
                && this.dateEphemeris[3].compareTo(date) < 0
                && this.dateEphemeris[this.dateEphemeris.length - 4].compareTo(date) > 0) {

            final double h = this.dateEphemeris[1].durationFrom(this.dateEphemeris[0]);
            final int i0 = (int) (date.durationFrom(this.dateEphemeris[0]) / h + 1 - interpOrder / 2);

            final double tk = date.durationFrom(this.dateEphemeris[i0]);

            final double[] td = new double[interpOrder];
            final double[] xT = new double[interpOrder];
            final double[] yT = new double[interpOrder];
            final double[] zT = new double[interpOrder];

            for (int i = 0; i < interpOrder; i++) {
                td[i] = this.dateEphemeris[i + i0].durationFrom(this.dateEphemeris[i0]);
                xT[i] = this.eph.getEphemeris().get(this.dateEphemeris[i + i0].getDate()).getPosition().getX();
                yT[i] = this.eph.getEphemeris().get(this.dateEphemeris[i + i0].getDate()).getPosition().getY();
                zT[i] = this.eph.getEphemeris().get(this.dateEphemeris[i + i0].getDate()).getPosition().getZ();
            }

            // computes the interpolated PVCoordinate
            final double[] y = new double[3];
            y[0] = PolynomialFunctionLagrangeForm.evaluate(td, xT, tk);
            y[1] = PolynomialFunctionLagrangeForm.evaluate(td, yT, tk);
            y[2] = PolynomialFunctionLagrangeForm.evaluate(td, zT, tk);

            final PVCoordinates pvs = new PVCoordinates(new Vector3D(y[0], y[1], y[2]), Vector3D.ZERO);
            // Compute transform
            final Transform t = this.eph.getReferenceFrame().getTransformTo(frame, date, this.framesConfiguration);
            // Return result
            return t.transformPVCoordinates(pvs);
        }

        // Cannot interpolate
        throw new IllegalArgumentException(
            "The date is not in the map and Lagrange interpolation is not possible");
    }

    /**
     * Computes the PV coordinates at a date using linear interpolator.
     * 
     * @param date
     *        the date
     * @param frame
     *        the frame
     * @return the PV coordinates at the given date
     * @throws PatriusException
     *         thrown if some frame conversion failed
     */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {

        if (this.eph.getEphemeris().containsKey(date)) {
            // no interpolation : the date is in the map
            final Transform t = this.eph.getReferenceFrame().getTransformTo(frame, date, this.framesConfiguration);
            return t.transformPVCoordinates(this.eph.getEphemeris().get(date));
        }

        if (this.eph.getEphemeris().size() > 1
                && this.dateEphemeris[0].compareTo(date) < 0
                && this.dateEphemeris[this.dateEphemeris.length - 1].compareTo(date) > 0) {
            // Initialization
            final double h = this.dateEphemeris[1].durationFrom(this.dateEphemeris[0]);
            final int i0 = (int) (date.durationFrom(this.dateEphemeris[0]) / h);
            final double tk = date.durationFrom(this.dateEphemeris[i0]);

            // computes the interpolated PVCoordinate
            final Vector3D pos0 = this.eph.getEphemeris().get(this.dateEphemeris[i0].getDate()).getPosition();
            final double x0 = pos0.getX();
            final double y0 = pos0.getY();
            final double z0 = pos0.getZ();

            final Vector3D pos1 = this.eph.getEphemeris().get(this.dateEphemeris[i0 + 1].getDate()).getPosition();
            final double x1 = pos1.getX();
            final double y1 = pos1.getY();
            final double z1 = pos1.getZ();

            final double x = (x1 - x0) / h * tk + x0;
            final double y = (y1 - y0) / h * tk + y0;
            final double z = (z1 - z0) / h * tk + z0;
            final PVCoordinates pvs = new PVCoordinates(new Vector3D(x, y, z), Vector3D.ZERO);
            // Compute transform
            final Transform t = this.eph.getReferenceFrame().getTransformTo(frame, date, this.framesConfiguration);

            // Return result
            return t.transformPVCoordinates(pvs);
        }

        // Cannot interpolate
        throw new IllegalArgumentException(DATE_NOT_IN_MAP_INTERPOLATION_NOT_POSSIBLE);
    }

    /** {@inheritDoc} */
    @Override
    public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
        return this.eph.getReferenceFrame();
    }
}
