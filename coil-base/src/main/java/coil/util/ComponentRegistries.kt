@file:JvmName("-ComponentRegistries")
@file:Suppress("DEPRECATION")

package coil.util

import coil.ComponentRegistry
import coil.decode.Decoder
import coil.fetch.Fetcher
import coil.map.Mapper
import okio.BufferedSource

@Suppress("UNCHECKED_CAST")
internal fun ComponentRegistry.mapData(data: Any): Any {
    var mappedData = data
    mappers.forEachIndices { (mapper, type) ->
        if (type.isAssignableFrom(mappedData::class.java) && (mapper as Mapper<Any, *>).handles(mappedData)) {
            mappedData = mapper.map(mappedData)
        }
    }
    return mappedData
}

@Suppress("UNCHECKED_CAST")
internal fun <T : Any> ComponentRegistry.requireFetcher(data: T): Fetcher<T> {
    val result = fetchers.findIndices { (fetcher, type) ->
        type.isAssignableFrom(data::class.java) && (fetcher as Fetcher<Any>).handles(data)
    }
    checkNotNull(result) { "Unable to fetch data. No fetcher supports: $data" }
    return result.first as Fetcher<T>
}

internal fun <T : Any> ComponentRegistry.requireDecoder(
    data: T,
    source: BufferedSource,
    mimeType: String?
): Decoder {
    val decoder = decoders.findIndices { it.handles(source, mimeType) }
    return checkNotNull(decoder) { "Unable to decode data. No decoder supports: $data" }
}
