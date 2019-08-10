package coil.collection

/** Adapted from Android's ArrayUtils and GrowingArrayUtils. */
internal object ArrayUtils {

    /**
     * Inserts an element into the array at the specified index, growing the array if there is no more room.
     *
     * @param array The array to which to append the element. Must NOT be null.
     * @param currentSize The number of elements in the array. Must be less than or equal to array.length.
     * @param element The element to insert.
     * @return the array to which the element was appended. This may be different than the given array.
     */
    fun insert(array: IntArray, currentSize: Int, index: Int, element: Int): IntArray {
        if (currentSize + 1 <= array.count()) {
            System.arraycopy(array, index, array, index + 1, currentSize - index)
            array[index] = element
            return array
        }

        val newArray = IntArray(growSize(currentSize))
        System.arraycopy(array, 0, newArray, 0, index)
        newArray[index] = element
        System.arraycopy(array, index, newArray, index + 1, array.count() - index)
        return newArray
    }

    /**
     * Given the current size of an array, returns an ideal size to which the array should grow.
     * This is typically double the given size, but should not be relied upon to do so in the
     * future.
     */
    fun growSize(currentSize: Int): Int {
        return if (currentSize <= 4) 8 else currentSize * 2
    }
}
