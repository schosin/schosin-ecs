package de.schosin.ecs.utils.collections;

import java.util.Arrays;
import java.util.function.IntConsumer;

public class BitVector {

    private static final int ADDRESS_BITS_PER_WORD = 6;
    private static final int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;

    private long[] words = { 0 };
    private int currentWord = -1;

    public BitVector() {
    }

    public BitVector(int length) {
        checkCapacity(1 + (length >>> ADDRESS_BITS_PER_WORD));
    }

    public BitVector(BitVector copyFrom) {
        this.words = Arrays.copyOf(copyFrom.words, copyFrom.words.length);
        this.currentWord = copyFrom.currentWord;
    }

    /**
     * Tests whether this vector contains all set bits of the other vector.
     * 
     * @return true, if this vector contains all set bits of other 
     */
    public boolean containsAll(BitVector other) {
        for (int i = 0; i < other.words.length; i++) {
            var otherValue = other.words[i];
            var value = i < words.length ? words[i] : 0L;

            var result = value & otherValue;
            if (result != otherValue) {
                return false;
            }

        }

        return true;
    }

    /**
     * Tests whether this BitVector contains none of the set bits of the other vector.
     * 
     * @return true, if this vector contains none of the set bits of other
     */
    public boolean containsNone(BitVector other) {
        for (int i = 0; i < words.length; i++) {
            var value = words[i];
            var otherValue = i < other.words.length ? other.words[i] : 0L;

            var result = value & otherValue;
            if (result != 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tests whether this BitVector contains atleast one set bit of the other vector.
     * 
     * @return true, if this contains atleast one set bit of other
     */
    public boolean containsSome(BitVector other) {
        for (int i = 0; i < other.words.length; i++) {
            var otherValue = other.words[i];
            var value = i < words.length ? words[i] : 0L;

            var result = value & otherValue;
            if (result != 0) {
                return true;
            }
        }

        return false;
    }

    public boolean get(int index) {
        if (this.currentWord == -1) {
            return false;
        }

        var wordIndex = wordIndex(index);
        return wordIndex < words.length && (words[wordIndex] & (1L << index)) != 0L;
    }

    public boolean unsafeGet(int index) {
        if (this.currentWord == -1) {
            return false;
        }

        var wordIndex = wordIndex(index);
        return (words[wordIndex] & (1L << index)) != 0L;
    }

    public void set(int index) {
        var wordIndex = wordIndex(index);
        checkCapacity(wordIndex);

        this.words[wordIndex] |= (1L << index);

        this.currentWord = this.currentWord > wordIndex ? currentWord : wordIndex;
    }

    /**
     * Sets the index and returns true if that index was not set before.
     * 
     * <p>
     * Use {@link #set(int)} if the return value is not used.
     * 
     * @return false if already set, true otherwise 
     */
    public boolean setAndReturn(int index) {
        var wordIndex = wordIndex(index);
        checkCapacity(wordIndex);

        // Return false if already set
        if ((words[wordIndex] & (1L << index)) != 0L) {
            return false;
        }

        // Set value and return true
        this.words[wordIndex] |= (1L << index);

        this.currentWord = this.currentWord > wordIndex ? currentWord : wordIndex;
        return true;
    }

    public void unsafeSet(int index) {
        var wordIndex = wordIndex(index);
        this.words[wordIndex] |= (1L << index);

        this.currentWord = this.currentWord > wordIndex ? currentWord : wordIndex;
    }

    public void setAll(BitVector other) {
        other.iterate(this::set);
    }

    private static int wordIndex(int bitIndex) {
        return bitIndex >>> ADDRESS_BITS_PER_WORD;
    }

    /**
     * Returns the first set bit at or after fromIndex. Allows for iteration over small
     * BitVectors. For larger BitVectors (highest bit at index 100 or more), use {@link #iterate(IntConsumer)}.
     * 
     * <pre>
     * {@snippet:
     *  var idx = vector.nextSetBit(0);
     *  while (idx > -1) {
     *      // calc
     *      idx = vector.nextSetBit(idx + 1);
     *  }
     * }
     * </pre>
     * 
     * @param fromIndex start index
     * @return index of first set bit or -1 if none
     */
    public int nextSetBit(int fromIndex) {
        if (currentWord == -1) {
            return -1;
        }

        var wordIndex = wordIndex(fromIndex);
        if (wordIndex >= words.length) {
            return -1;
        }

        var word = words[wordIndex] >>> fromIndex;
        if (word != 0) {
            return fromIndex + Long.numberOfTrailingZeros(word);
        }

        for (var i = wordIndex + 1; i < words.length; i++) {
            word = words[i];
            if (word != 0) {
                return i * BITS_PER_WORD + Long.numberOfTrailingZeros(word);
            }
        }

        return -1;
    }

    /**
     * Iterate over all set bits of this BitVector in order. 
     * Faster than {@link #nextSetBit(int)} for larger and denser BitVectors.
     * 
     * @param consumer called for every set bit
     */
    public void iterate(IntConsumer consumer) {
        if (currentWord == -1) {
            return;
        }

        var fromIndex = 0;
        var wordIndex = 0;

        while (wordIndex <= currentWord) {
            var word = words[wordIndex];
            while (word != 0) {
                // Find the position of the least significant set bit
                var index = fromIndex + Long.numberOfTrailingZeros(word);

                // Process index
                consumer.accept(index);

                // Clear the least significant set bit
                word &= word - 1;
            }

            // Move to the next word
            wordIndex++;
            fromIndex += BITS_PER_WORD;
        }
    }

    private void checkCapacity(int len) {
        if (len >= words.length) {
            synchronized (this) {
                if (len >= words.length) {
                    var newBits = new long[len + 1];
                    System.arraycopy(words, 0, newBits, 0, words.length);

                    words = newBits;
                }
            }
        }
    }

    public void clear(int index) {
        if (currentWord == -1) {
            return;
        }

        var wordIndex = wordIndex(index);
        if (wordIndex >= words.length) {
            return;
        }

        if ((this.words[wordIndex] &= ~(1L << index)) == 0 && wordIndex == this.currentWord) {
            for (this.currentWord = wordIndex - 1; this.currentWord > -1; --this.currentWord) {
                if (this.words[this.currentWord] > 0) {
                    return;
                }
            }

        }
    }

    public void unsafeClear(int index) {
        if (currentWord == -1) {
            return;
        }

        var wordIndex = wordIndex(index);

        if ((this.words[wordIndex] &= ~(1L << index)) == 0 && wordIndex == this.currentWord) {
            for (this.currentWord = wordIndex - 1; this.currentWord > 0; --this.currentWord) {
                if (this.words[this.currentWord] > 0) {
                    return;
                }
            }
        }
    }

    public void clear() {
        if (currentWord == -1) {
            return;
        }

        Arrays.fill(words, 0L);
        this.currentWord = -1;
    }

    public boolean isEmpty() {
        return this.currentWord == -1;
    }

    @Override
    public int hashCode() {
        if (currentWord == -1) {
            return 1;
        }

        final int prime = 31;
        int result = 1;

        for (int i = 0; i <= this.currentWord; i++) {
            var word = words[i];
            result = prime * result + (int) (word ^ (word >>> 32));
        }

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BitVector other = (BitVector) obj;
        if (this.currentWord != other.currentWord) {
            return false;
        }

        if (this.currentWord == -1) {
            return true;
        }

        for (int i = 0; i <= this.currentWord; i++) {
            if (words[i] != other.words[i]) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BitVector(");
        for (int i = 0; i <= currentWord; i++) {
            builder.append(words[i]);
            if (i < currentWord) {
                builder.append(", ");
            }
        }
        return builder.append(")").toString();
    }

}