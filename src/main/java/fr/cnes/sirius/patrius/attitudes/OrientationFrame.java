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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2799:18/05/2021:Suppression des pas de temps fixes codes en dur 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:133:02/10/2013:Javadoc completed
 * VERSION::DM:524:10/03/2016:serialization java doc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.TransformProvider;

/**
 * This class is a special implementation of the {@link Frame} class; it represents a dynamic orientation frame,
 * i.e. a dynamic frame whose orientation is defined by {@link IOrientationLaw}.
 * 
 * @serial OrientationFrame is serializable given a serializable {@link TransformProvider} (see {@link Frame})
 * 
 * @concurrency not thread safe
 * 
 * @concurrency.comment it is a Frame.
 * 
 * @author Julie Anton
 * 
 * @see OrientationTransformProvider
 * 
 * @version $Id: OrientationFrame.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 * 
 */
public class OrientationFrame extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = 7680325947511606746L;

    /** ORIENTATION_FRAME string. */
    private static final String ORIENTATION_FRAME = "orientation frame";

    /**
     * Builds the dynamic orientation frame using {@link AttitudeFrame} as the parent frame.
     * 
     * @param law
     *        the orientation law
     * @param frame
     *        the parent frame
     */
    public OrientationFrame(final IOrientationLaw law, final AttitudeFrame frame) {
        super(frame, new OrientationTransformProvider(law, frame), ORIENTATION_FRAME);
    }

    /**
     * Builds the dynamic orientation frame using {@link OrientationFrame} as the parent frame.
     * 
     * @param law
     *        the orientation law
     * @param frame
     *        the parent frame
     */
    public OrientationFrame(final IOrientationLaw law, final OrientationFrame frame) {
        super(frame, new OrientationTransformProvider(law, frame), ORIENTATION_FRAME);
    }

    /**
     * Builds the dynamic orientation frame using {@link AttitudeFrame} as the parent frame.
     * 
     * @param law
     *        the orientation law
     * @param frame
     *        the parent frame
     * @param spinDeltaT the delta-T used to compute spin by finite differences
     */
    public OrientationFrame(final IOrientationLaw law, final AttitudeFrame frame, final double spinDeltaT) {
        super(frame, new OrientationTransformProvider(law, frame, spinDeltaT), ORIENTATION_FRAME);
    }

    /**
     * Builds the dynamic orientation frame using {@link OrientationFrame} as the parent frame.
     * 
     * @param law
     *        the orientation law
     * @param frame
     *        the parent frame
     * @param spinDeltaT the delta-T used to compute spin by finite differences
     */
    public OrientationFrame(final IOrientationLaw law, final OrientationFrame frame, final double spinDeltaT) {
        super(frame, new OrientationTransformProvider(law, frame, spinDeltaT), ORIENTATION_FRAME);
    }

}
