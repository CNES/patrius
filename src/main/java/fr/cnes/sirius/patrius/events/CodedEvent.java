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
import java.util.SortedMap;
import java.util.SortedSet;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeStamped;

/**
 * <p>
 * This class represents an event occurrence.<br>
 * An event is identified by a code (its name), the date of occurrence, a string representing a comment and a flag that
 * indicates if the event is a "starting" event (i.e. an event that starts a phenomenon, like an eclipse) or an "ending"
 * event.<br>
 * Coded events are built by the {@link CodingEventDetector} during propagation using the
 * {@link CodingEventDetector#buildCodedEvent(fr.cnes.sirius.patrius.propagation.SpacecraftState, boolean)
 * buildCodedEvent} method.
 * </p>
 * 
 * @concurrency immutable
 * 
 * @see CodingEventDetector
 * @see CodedEventsList
 * 
 * @author Pierre Cardoso
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.1
 * 
 */
public final class CodedEvent implements TimeStamped, Comparable<CodedEvent>, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 200823340652597087L;
    /** Root int for hash code. */
    private static final int ROOTINT = 294;
    /** Efficient multiplier. */
    private static final int MULT = 31;
    /** Code for the event. */
    private final String code;
    /** Comment. */
    private final String comment;
    /** Date for the event occurrence. */
    private final AbsoluteDate date;
    /** True for a "starting" event, false for an "ending" event. */
    private final boolean startingEvent;
    /** Hash code. */
    private final int hashCodeNumber;

    /**
     * Constructor for the coded event.
     * 
     * @param codeIn
     *        code identifying the event.
     * @param commentIn
     *        comment for the event.
     * @param dateIn
     *        the {@link AbsoluteDate} of the event.
     * @param startingEventIn
     *        true if the event is a "starting" event, false if it is
     *        an "ending" event.
     */
    public CodedEvent(final String codeIn, final String commentIn,
        final AbsoluteDate dateIn, final boolean startingEventIn) {
        this.code = codeIn;
        this.comment = commentIn;
        this.date = dateIn;
        this.startingEvent = startingEventIn;
        // The instance is immutable, we compute the hash code
        // once and for all.
        this.hashCodeNumber = this.computeHashCode();
    }

    /**
     * Computes the hash code.<br>
     * The standard "clever" hash algorithm is used.
     * 
     * @return the hash code value.
     */
    private int computeHashCode() {
        int result = ROOTINT;
        // An efficient multiplier (JVM optimizes 31 * i as (i << 5) - 1 )
        final int effMult = MULT;
        // Good hashcode : it's the same
        // for "equal" CodedEvents, but
        // reasonably sure it's different otherwise.
        result = effMult * result + this.date.hashCode();
        result = effMult * result + (this.startingEvent ? 1 : 0);
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
     * @return the code of the event.
     */
    public String getCode() {
        return this.code;
    }

    /**
     * @return the comment of the event.
     */
    public String getComment() {
        return this.comment;
    }

    /**
     * @return the {@link AbsoluteDate} of the event.
     * @see fr.cnes.sirius.patrius.time.TimeStamped#getDate()
     */
    @Override
    public AbsoluteDate getDate() {
        return this.date;
    }

    /**
     * @return true if the event is a "starting" event or false if the event
     *         is a "ending" event.
     */
    public boolean isStartingEvent() {
        return this.startingEvent;
    }

    /**
     * Compares two {@link CodedEvent} instances.<br>
     * The ordering for {@link CodedEvent} is consistent with equals,
     * so that a {@link CodedEvent} can be used in any {@link SortedSet} or {@link SortedMap}.<br>
     * The ordering is :
     * <ul>
     * <li>the ordering of the events' dates if they differ.</li>
     * <li>if not, the alphabetical ordering of the code is used if they differ.</li>
     * <li>if not, the alphabetical ordering of the comment is used if they differ.</li>
     * <li>if not, the starting boolean is used (the starting event is "before").</li>
     * </ul>
     * 
     * @param event
     *        the {@link CodedEvent} to compare to
     * @return a negative integer, zero, or a positive integer as
     *         the {@link CodedEvent} is before, simultaneous, or after the specified
     *         event.
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final CodedEvent event) {
        final int rez;

        // first fast check
        if (event == this) {
            rez = 0;
            return rez;
        }

        // First we try to order by date.
        final int dateCompare = this.date.compareTo(event.getDate());
        if (dateCompare == 0) {
            // If the dates are the same, we order by the starting/ending
            // boolean, with the starting event "before".
            if (this.startingEvent == event.isStartingEvent()) {
                // If the dates and booleans are the same,
                // We use the alphabetical order of the codes.
                final int codeCompare = this.code.compareTo(event.getCode());
                if (codeCompare == 0) {
                    // If even the codes are the same,
                    // We use the alphabetical order of the comments.
                    rez = this.comment.compareTo(event.getComment());
                } else {
                    rez = codeCompare;
                }
            } else {
                // The starting event is before.
                rez = (this.startingEvent ? -1 : 1);
            }
        } else {
            rez = dateCompare;
        }
        return rez;
    }

    /**
     * Factory method for an undefined event, that still has a valid date.
     * 
     * @param date
     *        the {@link AbsoluteDate} of the event, needed even for an
     *        undefined event.
     * @param isStarting
     *        true when the event is a "starting" event.
     * @return a new {@link CodedEvent}.
     */
    public static CodedEvent buildUndefinedEvent(
                                                 final AbsoluteDate date, final boolean isStarting) {
        return new CodedEvent("UNDEFINED_EVENT", "undefined event",
            date, isStarting);
    }

    /**
     * Provides a String representation, based on this pattern :
     * "&lt;date&gt; - &lt;(Beg) or (End)&gt; - &lt;code&gt; : &lt;comment&gt;". <br>
     * (Beg) is for a starting event, (End) for an ending event.
     * 
     * @return the String representation
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final String startStr = (this.startingEvent ? "(Beg) - " : "(End) - ");
        return this.date.toString() + " - " + startStr + this.code.toString() + " : " + this.comment.toString();
    }

    /**
     * Checks if the instance represents the same {@link CodedEvent} as another
     * instance.
     * 
     * @param event
     *        other event
     * @return true if the instance and the other event are equals
     */
    @Override
    public boolean equals(final Object event) {

        if (event == this) {
            // first fast check
            return true;
        }

        if ((event != null) && (event instanceof CodedEvent)) {
            return this.code.equals(((CodedEvent) event).getCode())
                && this.comment.equals(((CodedEvent) event).getComment())
                && this.date.equals(((CodedEvent) event).getDate())
                && this.startingEvent == ((CodedEvent) event).isStartingEvent();
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return this.hashCodeNumber;
    }
}
