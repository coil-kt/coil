package coil.collection

/** Adapted from Android's ArrayUtils and GrowingArrayUtils. */
internal object ArrayUtils {

    /**
     * Inserts an element into the array at the specified index, growing the array if there is no more room.
     *
     * @param array The array to which to append the element.
     * @param currentSize The number of elements in the array. Must be less than or equal to array.size.
     * @param element The element to insert.
     * @return The array to which the element was appended. This may be different than the given array.
     */
    fun insert(array: IntArray, currentSize: Int, index: Int, element: Int): IntArray {
		// Fast path: insert into the given array.
        if (currentSize + 1 <= array.size) {
			array.copyInto(array, destinationOffset = index + 1, startIndex = index, endIndex = currentSize)
            array[index] = element
            return array
        }

		// Slow path: create a new, larger array and copy over the elements.
		val newSize = if (currentSize <= 4) 8 else currentSize * 2
        val newArray = IntArray(newSize)
		array.copyInto(newArray, endIndex = index)
        newArray[index] = element
		array.copyInto(newArray, destinationOffset = index + 1, startIndex = index, endIndex = array.size)
        return newArray
    }
}
