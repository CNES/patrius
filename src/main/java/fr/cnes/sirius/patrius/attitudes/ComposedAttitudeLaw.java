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
 * @history creation 21/12/2011
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2799:18/05/2021:Suppression des pas de temps fixes codes en dur 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:133:02/10/2013:Javadoc completed
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:583:11/03/2016:simplification of attitude laws architecture
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class represents a composed attitude law, defined by a main attitude law provider and a chained list of
 * orientation laws.<br>
 * The main attitude law provides, for a given date, the dynamic frame representing the spacecraft orientation; this
 * orientation is then progressively transformed using the orientation laws.
 * 
 * @concurrency conditionally thread-safe
 * 
 * @concurrency.comment is thread-safe if the AttitudeLaw attribute is.
 * 
 * @author Julie Anton
 * 
 * @see AttitudeFrame
 * @see AttitudeTransformProvider
 * 
 * @version $Id: ComposedAttitudeLaw.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 * 
 */
public class ComposedAttitudeLaw extends AbstractAttitudeLaw implements AttitudeLawModifier {

    /** Serializable UID. */
    private static final long serialVersionUID = -8663407950398620133L;

    /** Default value for delta-T used to compute spin by finite differences. */
    private static final double DEFAULT_SPIN_DELTAT = 0.1;

    /** Main attitude law. */
    private final AttitudeLaw mainLaw;

    /** Modifier laws. */
    private final List<IOrientationLaw> modifierLaws;

    /** The delta-T used to compute spin by finite differences. */
    private final double spinDeltaT;

    /**
     * Builds a composed attitude law from a man attitude law provider and a list of modifier orientation laws.
     * 
     * @param law
     *        the main attitude law
     * @param modifiers
     *        the orientation laws
     */
    @SuppressWarnings("PMD.LooseCoupling")
    public ComposedAttitudeLaw(final AttitudeLaw law, final LinkedList<IOrientationLaw> modifiers) {
        this(law, modifiers, DEFAULT_SPIN_DELTAT);
    }

    /**
     * Builds a composed attitude law from a man attitude law provider and a list of modifier orientation laws.
     * 
     * @param law
     *        the main attitude law
     * @param modifiers
     *        the orientation laws
     * @param spinDeltaT
     *        delta-t used for spin computation by finite differences
     */
    @SuppressWarnings("PMD.LooseCoupling")
    public ComposedAttitudeLaw(final AttitudeLaw law,
            final LinkedList<IOrientationLaw> modifiers,
            final double spinDeltaT) {
        super();
        this.mainLaw = law;
        this.modifierLaws = modifiers;
        this.spinDeltaT = spinDeltaT;
    }

    /** {@inheritDoc} */
    @Override
    public AttitudeLaw getUnderlyingAttitudeLaw() {
        return this.mainLaw;
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                final Frame frame) throws PatriusException {

        final AttitudeFrame attFrame = new AttitudeFrame(pvProv, this.mainLaw, frame);
        OrientationFrame orFrame = new OrientationFrame(this.modifierLaws.get(0), attFrame, spinDeltaT);
        final Iterator<IOrientationLaw> iterator = this.modifierLaws.iterator();
        iterator.next();
        while (iterator.hasNext()) {
            orFrame = new OrientationFrame(iterator.next(), orFrame, spinDeltaT);
        }
        final Transform t = frame.getTransformTo(orFrame, date, this.getSpinDerivativesComputation());

        return new Attitude(date, frame, t.getAngular());
    }
}
