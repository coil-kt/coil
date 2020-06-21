@file:JvmName("-ComponentRegistries")

package coil.util

import coil.ComponentRegistry
import coil.decode.Decoder
import coil.fetch.Fetcher
import coil.map.Mapper
import coil.map.MeasuredMapper
import coil.size.Size
import okio.BufferedSource

@Suppress("UNCHECKED_CAST")
internal inline fun ComponentRegistry.mapData(data: Any, lazySize: () -> Size): Any {
    var mappedData = data
    measuredMappers.forEachIndices { (type, mapper) ->
        if (type.isAssignableFrom(mappedData::class.java) && (mapper as MeasuredMapper<Any, *>).handles(mappedData)) {
            mappedData = mapper.map(mappedData, lazySize())
        }
    }
    mappers.forEachIndices { (type, mapper) ->
        if (type.isAssignableFrom(mappedData::class.java) && (mapper as Mapper<Any, *>).handles(mappedData)) {
            mappedData = mapper.map(mappedData)
        }
    }
    return mappedData
}

@Suppress("UNCHECKED_CAST")
internal fun <T : Any> ComponentRegistry.requireFetcher(data: T): Fetcher<T> {
    val result = fetchers.findIndices { (type, fetcher) ->
        type.isAssignableFrom(data::class.java) && (fetcher as Fetcher<Any>).handles(data)
    }
    checkNotNull(result) { "Unable to fetch data. No fetcher supports: $data" }
    return result.second as Fetcher<T>
}

internal fun <T : Any> ComponentRegistry.requireDecoder(
    data: T,
    source: BufferedSource,
    mimeType: String?
): Decoder {
    val decoder = decoders.findIndices { it.handles(source, mimeType) }
    return checkNotNull(decoder) { "Unable to decode data. No decoder supports: $data" }
}
