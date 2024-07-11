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
 * @history created 27/01/12
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:922:15/11/2017: Serializable interface implementation
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.events;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;

/**
 * <p>
 * This class represents an observable phenomenon.
 * </p>
 * <p>
 * A phenomenon is represented by a time interval and two boundaries; the boundaries can coincide with two
 * {@link CodedEvent}, or just be computational boundaries.
 * </p>
 * 
 * @concurrency immutable
 * 
 * @author Pierre Cardoso
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public final class Phenomenon implements Comparable<Phenomenon>, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -2497792222868304466L;

    /** Root int for hash code */
    private static final int ROOTINT = 401;

    /** {@link CodedEvent} at the beginning of phenomenon. */
    private final CodedEvent beginEvent;

    /**
     * True if the beginning is a well-defined date, false if it is a computation boundary without meaning.
     */
    private final boolean beginDefined;

    /** {@link CodedEvent} at the end of the phenomenon. */
    private final CodedEvent endEvent;

    /**
     * True if the end is a well-defined date, false if it is a computation boundary without meaning.
     */
    private final boolean endDefined;

    /** Timespan of the phenomenon. */
    private final AbsoluteDateInterval timespan;

    /** Code describing the phenomenon. */
    private final String code;

    /** Comment for the phenomenon. */
    private final String comment;

    /** Hash code. */
    private final int hashCodeNumber;

    /**
     * Constructor for the {@link Phenomenon} instance.<br>
     * The first and second {@link CodedEvent} boundaries can be in any time order.
     * 
     * @param boundaryOne
     *        {@link CodedEvent} representing the first boundary.
     * @param boundaryOneDefined
     *        true if the first boundary value is defined, false if is just a meaningless placeholder.
     * @param boundaryTwo
     *        {@link CodedEvent} representing the second boundary.
     * @param boundaryTwoDefined
     *        true if the second boundary value is defined, false if is just a meaningless placeholder.
     * @param codeIn
     *        the phenomenon code
     * @param commentIn
     *        a comment for the phenomenon
     */
    public Phenomenon(final CodedEvent boundaryOne, final boolean boundaryOneDefined, final CodedEvent boundaryTwo,
        final boolean boundaryTwoDefined, final String codeIn, final String commentIn) {

        if (boundaryOne.getDate().compareTo(boundaryTwo.getDate()) <= 0) {
            this.beginEvent = boundaryOne;
            this.beginDefined = boundaryOneDefined;
            this.endEvent = boundaryTwo;
            this.endDefined = boundaryTwoDefined;
        } else {
            this.beginEvent = boundaryTwo;
            this.beginDefined = boundaryTwoDefined;
            this.endEvent = boundaryOne;
            this.endDefined = boundaryOneDefined;
        }

        final IntervalEndpointType beginIn = IntervalEndpointType.OPEN;
        final IntervalEndpointType endIn = IntervalEndpointType.OPEN;

        this.timespan = new AbsoluteDateInterval(beginIn, this.beginEvent.getDate(), this.endEvent.getDate(), endIn);

        this.code = codeIn;

        this.comment = commentIn;

        this.hashCodeNumber = this.computeHashCode();
    }

    /**
     * Computes the hash code.<br>
     * The standard "clever" hash algorithm is used.
     * 
     * @return the hash code value.
     */
    private int computeHashCode() {
        // A not zero random "root int"
        int result = ROOTINT;
        // An efficient multiplier (JVM optimizes 31 * i as (i << 5) - 1 )
        final int effMult = 31;
        // Good hashcode : it's the same
        // for "equal" Phenomena, but
        // reasonably sure it's different otherwise.
        result = effMult * result + this.beginEvent.hashCode();
        result = effMult * result + this.endEvent.hashCode();
        result = effMult * result + (this.beginDefined ? 1 : 0);
        result = effMult * result + (this.endDefined ? 1 : 0);
        result = effMult * result + this.code.hashCode();
        result = effMult * result + this.comment.hashCode();
        return result;
        // DEVELOPER NOTE
        // If performance issues arise, due to the
        // mandatory hash code computation during creation,
        // the hash code computation should be lazily delayed
        // using a "private volatile int hashcode", computed
        // the first time hash code is called.
        // That would turn this class from "immutable" to "thread-safe".
    }

    /**
     * Get the starting event.
     * 
     * @return the {@link CodedEvent} instance.
     */
    public CodedEvent getStartingEvent() {
        return this.beginEvent;
    }

    /**
     * Get the ending event.
     * 
     * @return the {@link CodedEvent} instance.
     */
    public CodedEvent getEndingEvent() {
        return this.endEvent;
    }

    /**
     * True if the first boundary value is defined.
     * 
     * @return true or false
     */
    @SuppressWarnings("PMD.BooleanGetMethodName")
    public boolean getStartingIsDefined() {
        return this.beginDefined;
    }

    /**
     * True if the second boundary value is defined.
     * 
     * @return true or false
     */
    @SuppressWarnings("PMD.BooleanGetMethodName")
    public boolean getEndingIsDefined() {
        return this.endDefined;
    }

    /**
     * Get the timespan as an AbsoluteDateInterval.
     * 
     * @return the AbsoluteDateInterval instance.
     */
    public AbsoluteDateInterval getTimespan() {
        return this.timespan;
    }

    /**
     * @return the phenomenon code
     */
    public String getCode() {
        return this.code;
    }

    /**
     * @return the phenomenon comment
     */
    public String getComment() {
        return this.comment;
    }

    /**
     * Compares the Phenomenon instances.<br>
     * If the beginning events differ, their comparison is the result. If not, if the ending events differ, they are
     * compared. If not, the status of the boundaries is used to order<br>
     * Please note that the CodedEvent comparison uses more than the dates to order the CodedEvents.
     * 
     * @param o
     *        the {@link Phenomenon} to compare to
     * 
     * @return a negative integer, zero, or a positive integer as the {@link Phenomenon} is before, simultaneous, or
     *         after the specified one.
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final Phenomenon o) {

        // Quick exit
        if (this.equals(o)) {
            return 0;
        }

        int rez = 0;
        final int compBegins = this.beginEvent.compareTo(o.getStartingEvent());
        final int compEnds = this.endEvent.compareTo(o.getEndingEvent());

        if (compBegins == 0) {
            if (compEnds == 0) {
                // If the phenomena begin and end at the same "time",
                // we order on the code or comment
                rez = this.compareCodeComment(o);
                if (rez == 0) {
                    // If the phenomena have everything in common
                    // up to now, we compare the definedness of the boundaries.
                    rez = this.compareDefinedness(o);
                }
            } else {
                // If the phenomena begin at the same "time",
                // we order the shorter one "before".
                rez = compEnds;
            }
        } else {
            // If the beginnings differ, we order using them.
            rez = compBegins;
        }
        return rez;
    }

    /**
     * Subroutine comparing code and comment.
     * 
     * @param o
     *        the compared to instance.
     * @return comparison result.
     */
    private int compareCodeComment(final Phenomenon o) {
        int rez = this.code.compareTo(o.getCode());
        if (rez == 0) {
            // If the codes are the same, we resort to the comments.
            rez = this.comment.compareTo(o.getComment());
        }
        return rez;
    }

    /**
     * Subroutine comparing the boundaries' definedness.
     * 
     * @param o
     *        the compared to instance.
     * @return comparison result.
     */
    private int compareDefinedness(final Phenomenon o) {
        // At this point everything is identical in the phenomena
        // save their definedness.
        // The order is the following :
        // - if the starting definedness differ, the defined start comes first.
        // - if the ending definedness differ, the defined ending comes first.
        int rez = 0;
        final boolean otherStartIsDefined = o.getStartingIsDefined();
        final boolean otherEndIsDefined = o.getEndingIsDefined();

        if (this.beginDefined == otherStartIsDefined) {
            if (this.endDefined != otherEndIsDefined) {
                rez = (this.endDefined ? -1 : 1);
            }
            // There is no "else" because otherwise the phenomena are exactly the same and this case has already been
            // covered at the beginning of the method compareTo(Phenomenon) with the method equals(Phenomenon).
        } else {
            rez = (this.beginDefined ? -1 : 1);
        }

        return rez;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.code + " [ " + this.beginEvent.toString() + " ; " + this.endEvent.toString() + " ] : "
            + this.comment;
    }

    /**
     * Checks if the instance represents the same {@link Phenomenon} as another instance.
     * 
     * @param phenomenon
     *        other phenomenon
     * @return true if the instance and the other are equals
     */
    @Override
    public boolean equals(final Object phenomenon) {

        final boolean exit;
        // return true if the object is exactly the same.
        if (phenomenon == this) {
            // first fast check
            exit = true;
        } else {
            if ((phenomenon != null) && (phenomenon instanceof Phenomenon)) {
                //cast object in Phenomenon
                final Phenomenon cPheno = (Phenomenon) phenomenon;
                final boolean roiz =
                    this.code.equals(cPheno.getCode()) && this.comment.equals(cPheno.getComment())
                        && this.beginEvent.getDate().equals(cPheno.getStartingEvent().getDate())
                        && this.endEvent.getDate().equals(cPheno.getEndingEvent().getDate())
                        && this.beginDefined == cPheno.getStartingIsDefined()
                        && this.endDefined == cPheno.getEndingIsDefined();
                exit = roiz;
            } else {
                exit = false;
            }
        }
        return exit;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return this.hashCodeNumber;
    }
}
