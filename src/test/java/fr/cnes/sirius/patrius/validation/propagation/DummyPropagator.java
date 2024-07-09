/**
 * 
 * Copyright 2011-2017 CNES
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
package fr.cnes.sirius.patrius.validation.propagation;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * A simple dummy propagator used for some test purpose.
 * </p>
 * <p>
 * This DummyPropagator stores a PVCoordinates, a date and a frame. It does nothing but returns stored PVCoordinates at
 * stored date and in stored frame. It has been created to provide a non-time based propagator.
 * </p>
 * 
 * @concurrency immutable
 * 
 * @author Sylvain VRESK
 * 
 * @version $Id: DummyPropagator.java 17926 2017-09-11 13:54:24Z bignon $
 * 
 * @since version when the file was created TODO
 * 
 */
public class DummyPropagator implements PVCoordinatesProvider {

    /**
     * The stored PVCoordinates.
     */
    private final PVCoordinates pvc;

    /**
     * The stored AbsoluteDate.
     */
    private final AbsoluteDate date;

    /**
     * The stored frame.
     */
    private final Frame frame;

    /**
     * Creates a new instance.
     * 
     * @param pvc
     *        the PVCoordinates
     * @param date
     *        the date
     * @param frame
     *        the frame
     * 
     * @since 1.0
     */
    public DummyPropagator(final PVCoordinates pvc, final AbsoluteDate date, final Frame frame) {
        super();
        this.pvc = pvc;
        this.date = date;
        this.frame = frame;
    }

    /**
     * TODO description. Begins with a third-person verb (e.g. computes this)
     * possibly : explain the algorithm used (what it does? how? what are its limits?)
     * 
     * @precondition
     * 
     * @return TODO give what each variable represents and their validity interval
     */
    public PVCoordinates getPVCoordinates() {
        return this.pvc;
    }

    /**
     * TODO description. Begins with a third-person verb (e.g. computes this)
     * possibly : explain the algorithm used (what it does? how? what are its limits?)
     * 
     * @precondition
     * 
     * @return TODO give what each variable represents and their validity interval
     */
    public AbsoluteDate getDate() {
        return this.date;
    }

    /**
     * TODO description. Begins with a third-person verb (e.g. computes this)
     * possibly : explain the algorithm used (what it does? how? what are its limits?)
     * 
     * @precondition
     * 
     * @return TODO give what each variable represents and their validity interval
     */
    public Frame getFrame() {
        return this.frame;
    }

    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
        if (date.compareTo(this.date) != 0) {
            throw new IllegalArgumentException();
        }
        if (!frame.equals(this.frame)) {
            throw new IllegalArgumentException();
        }
        return this.pvc;
    }

}
