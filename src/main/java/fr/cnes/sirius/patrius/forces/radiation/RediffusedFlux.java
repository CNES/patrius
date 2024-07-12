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
 * @history creation 25/05/2012
 *
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::FA:318:05/11/2014:anomalies correction for class RediffusedFlux
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::FA:1324:09/05/2018:Comment the model limits and the unique source possibility (0,0)
 * VERSION::DM:1489:07/06/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.radiation;

import java.util.Arrays;

import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * creating a set of solar pressure rediffused by the earth for a satellite position.
 * 
 * @concurrency conditionally thread-safe
 * @concurrency.comment thread-safe if all constructor parameters are too.
 * 
 * @author ClaudeD
 * 
 * @version $Id: RediffusedFlux.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.2
 * 
 */
public class RediffusedFlux {

    /** 0.25 */
    private static final double QUARTER = 0.25;
    /** Equatorial radius. */
    private static final double EQUATORIAL_RADIUS = Constants.GRS80_EARTH_EQUATORIAL_RADIUS;
    /** Equatorial radius * Equatorial radius . */
    private static final double SQUARE_EQUATORIAL_RADIUS = EQUATORIAL_RADIUS * EQUATORIAL_RADIUS;
    /** Astronomical unit for this model power two */
    private static final double SQUARE_OBELIX_ASTRONOMICAL_UNIT = 149597870660. * 149597870660.;
    /** Zero elementary flux */
    private static final ElementaryFlux ZERO_ELEMENTARY_FLUX = new ElementaryFlux(Vector3D.ZERO, 0., 0.);

    /** Flux. */
    private final ElementaryFlux[] flux;
    /** calculation indicator of the albedo force */
    private final boolean albedo;
    /** calculation indicator of the infrared force */
    private final boolean ir;

    /**
     * Default constructor of rediffused flux.
     * 
     * @param nCorona number of corona
     * @param nMeridian number of meridian
     * @param bodyFrame frame of flux
     * @param sunProvider sun PV coordinates provider
     * @param satProvider satellite PV coordinates provider
     * @param d date of computing
     * @param model emissivity model
     * @throws PatriusException when orekit error occurred
     * 
     * @since 1.2
     */
    public RediffusedFlux(final int nCorona, final int nMeridian, final Frame bodyFrame,
        final CelestialBody sunProvider, final PVCoordinatesProvider satProvider,
        final AbsoluteDate d, final IEmissivityModel model) throws PatriusException {
        this(nCorona, nMeridian, bodyFrame, sunProvider, satProvider, d, model, true, true);
    }

    /**
     * Generic constructor of rediffused flux.
     * 
     * The number of coronas and meridians should be superior to (5,5) for a good earth
     * discretization. Otherwise the acceleration is largely overestimated.
     * 
     * @param nCorona number of coronas
     * @param nMeridian number of meridians
     * @param bodyFrame frame of flux
     * @param sun sun PV coordinates provider
     * @param satProvider satellite PV coordinates provider
     * @param dDate date of computing
     * @param model emissivity model
     * @param inIr computing indicator of the infrared force
     * @param inAlbedo computing indicator of the albedo force
     * @throws PatriusException when orekit error occurred
     * 
     * @since 1.2
     */
    public RediffusedFlux(final int nCorona, final int nMeridian, final Frame bodyFrame,
        final CelestialBody sun, final PVCoordinatesProvider satProvider,
        final AbsoluteDate dDate, final IEmissivityModel model, final boolean inIr,
        final boolean inAlbedo) throws PatriusException {

        this.ir = inIr;
        this.albedo = inAlbedo;
        // Checking if the number of coronas or meridians is positive or the
        // the number of coronas or meridians are (0,0)
        RediffusedFlux.checkInput(nCorona, nMeridian);

        // transform into bobyFrame
        final PVCoordinates sunPV = sun.getPVCoordinates(dDate, bodyFrame);
        final PVCoordinates satPV = satProvider.getPVCoordinates(dDate, bodyFrame);

        // number of surfaces
        final int nSurfaces = nCorona * nMeridian + 1;
        this.flux = new ElementaryFlux[nSurfaces];

        // shadow test (if no infrared computing and if the satellite is in the umbra ==> flux = 0)
        boolean flagComputing = true;
        if (!this.ir) {
            final Vector3D vEarthSat = satPV.getPosition();
            final Vector3D vEarthSatUnit = vEarthSat.normalize();
            final Vector3D vEarthSunUnit = sunPV.getPosition().normalize();
            final double valuea = MathLib.divide(EQUATORIAL_RADIUS, vEarthSat.getNorm());
            final double a = MathLib.asin(MathLib.min(1.0, valuea));
            final double valueb = -(vEarthSatUnit.getX() * vEarthSunUnit.getX()
                + vEarthSatUnit.getY() * vEarthSunUnit.getY() + vEarthSatUnit.getZ()
                * vEarthSunUnit.getZ());
            final double b = MathLib.acos(MathLib.min(1.0, MathLib.max(-1.0, valueb)));
            if (a >= b) {
                flagComputing = false;
                Arrays.fill(this.flux, ZERO_ELEMENTARY_FLUX);
            }
        }
        if (flagComputing) {
            this.computingRediffusedFlux(nSurfaces, nCorona, nMeridian, model, bodyFrame, dDate, satPV,
                sunPV);
        }
    }

    /**
     * Check input.
     * 
     * @param nCorona number of coronas
     * @param nMeridian number of meridians
     */
    private static void checkInput(final int nCorona, final int nMeridian) {
        // Checking if the number of coronas or meridians is positive or the
        // the number of coronas or meridians are (0,0)
        final boolean isUnacceptedValues = (nCorona == 0 && nMeridian != 0)
            || (nCorona != 0 && nMeridian == 0);
        if (nCorona < 0 || nMeridian < 0 || isUnacceptedValues) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Computing of rediffused fluxes.
     * 
     * Creation of a set of basic rediffused solar flux (direction, albedo and infrared
     * intensities). The terrestrial flux is considered to be a superposition of parallel fluxes
     * resulting from a certain number of elementary surfaces. The number of elementary surfaces
     * depends of a number of meridians and coronas. Each elementary flux contains a direction, a
     * albedo intensity and a infrared intensity.
     * 
     * All computations are made in the terrestrial frame.
     * 
     * @param nSurfaces number of surfaces
     * @param nCorona number of coronas
     * @param nMeridian number of meridians
     * @param model emissivity model
     * @param bodyFrame frame of flux
     * @param dDate date of computing
     * @param satPV satellite PV coordinates
     * @param sunPV sun PV coordinates
     * @throws PatriusException when orekit error occurred
     * 
     * @see OBELIX library
     * 
     * @since 1.2
     */
    private void computingRediffusedFlux(final int nSurfaces, final int nCorona, final int nMeridian,
                                         final IEmissivityModel model, final Frame bodyFrame, final AbsoluteDate dDate,
                                         final PVCoordinates satPV, final PVCoordinates sunPV) throws PatriusException {

        // Variables initialization
        double latitudeSurface;
        double longitudeSurface;
        final double[] dlongSurfacet = new double[2];
        final double dEarthSat = satPV.getPosition().getNorm();

        // nadir point in geodetic frame
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(EQUATORIAL_RADIUS, 0., bodyFrame);
        final GeodeticPoint geoSat = earth.transform(satPV.getPosition(), bodyFrame, dDate);
        // Geodesic coordinates of nadir point
        final double latitudeNadir = geoSat.getLatitude();
        final double longitudeNadir = geoSat.getLongitude();
        final double[] sincos = MathLib.sinAndCos(latitudeNadir);
        final double sinlatitudeNadir = sincos[0];
        final double coslatitudeNadir = sincos[1];

        // Number of meridians
        final int nMeridian2 = (nMeridian + 1) / 2;

        // Loop on coronas
        final double[] alpha = new double[nCorona + 1];
        final double c = (dEarthSat - EQUATORIAL_RADIUS) / (dEarthSat * nSurfaces);
        for (int i = 0; i < nCorona + 1; i++) {
            final double value = 1. - c * (1. + nMeridian * i);
            alpha[i] = MathLib.acos(MathLib.min(1.0, MathLib.max(-1.0, value)));
        }

        // sub-satellite cap
        int iFlux = 0;
        double[] e = model.getEmissivity(dDate, latitudeNadir, longitudeNadir);
        // Flux
        this.flux[iFlux] = this.getElementaryFlux(nSurfaces, e[0], e[1], latitudeNadir, longitudeNadir,
            sunPV, satPV);

        // other caps
        for (int i = 0; i < nCorona; i++) {
            final double phi = (alpha[i] + alpha[i + 1]) / 2.;
            final double[] sincosPhi = MathLib.sinAndCos(phi);
            final double sinphi = sincosPhi[0];
            final double cosphi = sincosPhi[1];

            // first meridian
            iFlux = iFlux + 1;
            latitudeSurface = latitudeNadir + phi;
            longitudeSurface = longitudeNadir;
            if (latitudeSurface > MathUtils.HALF_PI) {
                latitudeSurface = FastMath.PI - latitudeSurface;
                longitudeSurface = FastMath.PI + longitudeSurface;
            }
            // get the albedo and infrared emissivity:
            e = model.getEmissivity(dDate, latitudeSurface, longitudeSurface);
            // get the elementary rediffused flux:
            this.flux[iFlux] = this.getElementaryFlux(nSurfaces, e[0], e[1], latitudeSurface,
                longitudeSurface, sunPV, satPV);

            // other meridians (in pairs)
            for (int j = 1; j < nMeridian2; j++) {
                final double cosMeridian = MathLib.cos(MathUtils.TWO_PI
                    * (j / (double) nMeridian));
                final double value = cosphi * sinlatitudeNadir + sinphi * coslatitudeNadir
                    * cosMeridian;
                latitudeSurface = MathLib.asin(MathLib.min(1.0, MathLib.max(-1.0, value)));
                final double cosLatitudeSurface = MathLib.cos(latitudeSurface);
                if (MathLib.cos(cosLatitudeSurface) == 0.) {
                    // latitude cosine is zero:
                    throw new PatriusException(PatriusMessages.ZERO_COSLAT_SURFACE);
                }
                final double dlongSurface = MathLib.acos(MathLib.divide((cosphi
                    * coslatitudeNadir - sinphi * sinlatitudeNadir * cosMeridian),
                    cosLatitudeSurface));
                dlongSurfacet[0] = longitudeNadir + dlongSurface;
                dlongSurfacet[1] = longitudeNadir - dlongSurface;
                for (int k = 0; k < 2; k++) {
                    iFlux = iFlux + 1;
                    longitudeSurface = dlongSurfacet[k];
                    // get the albedo and infrared emissivity:
                    e = model.getEmissivity(dDate, latitudeSurface, longitudeSurface);
                    // get the elementary rediffused flux:
                    this.flux[iFlux] = this.getElementaryFlux(nSurfaces, e[0], e[1], latitudeSurface,
                        longitudeSurface, sunPV, satPV);
                }
            }

            // Last meridian if even
            if ((nMeridian + 1) / 2 == (nMeridian / 2)) {
                iFlux = iFlux + 1;
                latitudeSurface = latitudeNadir - phi;
                longitudeSurface = longitudeNadir;
                if (latitudeSurface < -MathUtils.HALF_PI) {
                    latitudeSurface = -FastMath.PI - latitudeSurface;
                    longitudeSurface = FastMath.PI + longitudeSurface;
                }
                e = model.getEmissivity(dDate, latitudeSurface, longitudeSurface);
                // Flux
                this.flux[iFlux] = this.getElementaryFlux(nSurfaces, e[0], e[1], latitudeSurface,
                    longitudeSurface, sunPV, satPV);
            }
        }
    }

    /**
     * Computing of one elementary rediffused flux.
     * 
     * @param numSurface total number of elementary surfaces
     * @param eA albedo emissivity.
     * @param eIR infrared emissivity.
     * @param latitudeSurface latitude of elementary surface
     * @param longitudeSurface longitude of elementary surface
     * @param sunPV sun position
     * @param satPV satellite position
     * @return elementaryFlux elementary rediffused flux
     * 
     * @see OBELIX library
     * 
     * @since 1.2
     */
    private ElementaryFlux getElementaryFlux(final double numSurface, final double eA,
                                             final double eIR, final double latitudeSurface,
                                             final double longitudeSurface,
                                             final PVCoordinates sunPV, final PVCoordinates satPV) {

        if (!this.albedo && !this.ir) {
            // elementary flux without albedo nor IR:
            return new ElementaryFlux(Vector3D.ZERO, 0., 0.);
        }

        // Sin/Cos lon/lat
        final double[] sincosLon = MathLib.sinAndCos(longitudeSurface);
        final double sinLon = sincosLon[0];
        final double cosLon = sincosLon[1];
        final double[] sincosLat = MathLib.sinAndCos(latitudeSurface);
        final double sinLat = sincosLat[0];
        final double cosLat = sincosLat[1];

        // Cartesian coordinates
        final double x = EQUATORIAL_RADIUS * cosLon * cosLat;
        final double y = EQUATORIAL_RADIUS * sinLon * cosLat;
        final double z = EQUATORIAL_RADIUS * sinLat;
        final Vector3D vEarthSurface = new Vector3D(x, y, z);
        final Vector3D vEarthSurfaceUnit = vEarthSurface.normalize();

        final double dEarthSat = satPV.getPosition().getNorm();
        final Vector3D vSurfaceSat = satPV.getPosition().subtract(vEarthSurface);

        // direction of the elementary flux
        final Vector3D vSurfaceSatUnit = vSurfaceSat.normalize();

        final double dSurfaceSat2 = vSurfaceSat.getNormSq();
        final Vector3D vEarthSunUnit = sunPV.getPosition().normalize();
        final double dEarthSun2 = sunPV.getPosition().getNormSq();

        final double cosKh = vSurfaceSatUnit.getX() * vEarthSurfaceUnit.getX()
            + vSurfaceSatUnit.getY() * vEarthSurfaceUnit.getY() + vSurfaceSatUnit.getZ()
            * vEarthSurfaceUnit.getZ();
        final double c = MathLib.divide(2. * cosKh * SQUARE_EQUATORIAL_RADIUS
            * Constants.CONST_SOL_N_M2 * (dEarthSat - EQUATORIAL_RADIUS), dEarthSat
            * numSurface);

        // Pressure parameters creation
        final double albedoPressure;
        final double infraRedPressure;

        // Albedo pressure
        if (this.albedo) {
            final double cosPs = vEarthSunUnit.getX() * vEarthSurfaceUnit.getX()
                + vEarthSunUnit.getY() * vEarthSurfaceUnit.getY() + vEarthSunUnit.getZ()
                * vEarthSurfaceUnit.getZ();
            if (cosPs > 0.) {
                albedoPressure = MathLib.divide(eA * c * cosPs * SQUARE_OBELIX_ASTRONOMICAL_UNIT,
                    dEarthSun2 * dSurfaceSat2);
            } else {
                // the albedo is zero:
                albedoPressure = 0.;
            }
        } else {
            // the albedo is zero:
            albedoPressure = 0.;
        }

        // Infrared pressure
        if (this.ir) {
            infraRedPressure = MathLib.divide(QUARTER * eIR * c, dSurfaceSat2);
        } else {
            infraRedPressure = 0.;
        }

        return new ElementaryFlux(vSurfaceSatUnit, albedoPressure, infraRedPressure);
    }

    /**
     * getFlux : return all elementary rediffused fluxes
     * 
     * @return rediffused fluxes in bodyFrame frame
     * 
     * @since 1.2
     */
    public ElementaryFlux[] getFlux() {
        return getFlux(Transform.IDENTITY);
    }

    /**
     * Return all elementary rediffused fluxes.
     * 
     * @param t
     *        the transform from body frame to the wanted frame
     * @return rediffused fluxes in the wanted frame
     * 
     * @since 4.10
     */
    public ElementaryFlux[] getFlux(final Transform t) {
        final ElementaryFlux[] outFlux;
        if (t == Transform.IDENTITY) {
            // No need to transform the fluxes
            outFlux = this.flux.clone();
        } else {
            outFlux = new ElementaryFlux[this.flux.length];
            for (int i = 0; i < outFlux.length; i++) {
                // Transform each flux
                final ElementaryFlux instanceFlux = this.flux[i];
                outFlux[i] = new ElementaryFlux(t.transformVector(instanceFlux.getDirFlux()),
                    instanceFlux.getAlbedoPressure(), instanceFlux.getInfraRedPressure());
            }
        }
        // Return all elementary rediffused fluxes
        return outFlux;
    }

    /**
     * Calculation indicator of the albedo force
     * 
     * @return if albedo force is computed
     */
    public boolean isAlbedo() {
        return this.albedo;
    }

    /**
     * Calculation indicator of the infrared force
     * 
     * @return the ir force is computed
     */
    public boolean isIr() {
        return this.ir;
    }
}
