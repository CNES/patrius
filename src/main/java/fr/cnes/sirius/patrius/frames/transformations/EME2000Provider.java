/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2017 CNES
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
 */
/*
 *
 * HISTORY
* VERSION:4.7:DM:DM-2590:18/05/2021:Configuration des TransformProvider 
* VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:524:25/05/2016:serialization java doc
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.transformations;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;

/**
 * EME2000 frame : mean equator at J2000.0.
 * <p>
 * This frame was the standard inertial reference prior to GCRF. It was defined using Lieske precession-nutation model
 * for Earth. This frame has been superseded by GCRF which is implicitly defined from a few hundred quasars coordinates.
 * </p>
 * <p>
 * The transformation between GCRF and EME2000 is a constant rotation bias.
 * </p>
 *
 * <p>Spin derivative, when computed, is always 0 by definition.</p>
 * <p>Frames configuration is unused.</p>
 * 
 * @serial serializable.
 * @author Luc Maisonobe
 */
public final class EME2000Provider extends FixedTransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = -6134137187835219727L;

    /** Obliquity of the ecliptic. */
    private static final double EPSILON_0 = 84381.448 * Constants.ARC_SECONDS_TO_RADIANS;

    /** Bias in longitude. */
    private static final double D_PSI_B = -0.041775 * Constants.ARC_SECONDS_TO_RADIANS;

    /** Bias in obliquity. */
    private static final double D_EPSILON_B = -0.0068192 * Constants.ARC_SECONDS_TO_RADIANS;

    /** Right Ascension of the 2000 equinox in ICRS frame. */
    private static final double ALPHA_0 = -0.0146 * Constants.ARC_SECONDS_TO_RADIANS;

    /**
     * Simple constructor.
     */
    public EME2000Provider() {

        // build the bias transform
        super(new Transform(AbsoluteDate.J2000_EPOCH,
            (new Rotation(Vector3D.PLUS_I, D_EPSILON_B).
                applyTo(new Rotation(Vector3D.PLUS_J, -D_PSI_B * MathLib.sin(EPSILON_0)).
                    applyTo(new Rotation(Vector3D.PLUS_K, -ALPHA_0)))).revert()));

    }

}
