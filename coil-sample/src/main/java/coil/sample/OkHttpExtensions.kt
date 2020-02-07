package coil.sample

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.JELLY_BEAN
import android.os.Build.VERSION_CODES.LOLLIPOP
import androidx.annotation.RequiresApi
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

private const val TLS_1_2 = "TLSv1.2"

private val TLS_1_2_ONLY = arrayOf(TLS_1_2)

private val CONNECTION_SPEC_TLS_1_2_ONLY = run {
    ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
        .tlsVersions(TlsVersion.TLS_1_2)
        .build()
}

/**
 * Force the [OkHttpClient] to only accept TLS 1.2 connections.
 *
 * This enables TLS 1.2 support on API 16+.
 */
@RequiresApi(JELLY_BEAN)
fun OkHttpClient.Builder.forceTls12(): OkHttpClient.Builder {
    // TLS 1.2 is enabled by default on Lollipop and above.
    if (SDK_INT >= LOLLIPOP) return this

    try {
        val sslContext = SSLContext.getInstance(TLS_1_2)
        val trustManager = getDefaultTrustManager()
        sslContext.init(null, null, null)
        val socketFactory = Tls12SocketFactory(sslContext.socketFactory)

        // If we don't find the X509TrustManager, let OkHttp try to find it.
        if (trustManager != null) {
            sslSocketFactory(socketFactory, trustManager)
        } else {
            @Suppress("DEPRECATION")
            sslSocketFactory(socketFactory)
        }

        connectionSpecs(listOf(CONNECTION_SPEC_TLS_1_2_ONLY))
    } catch (_: Exception) {}

    return this
}

/** Find and initialize the default trust manager. */
private fun getDefaultTrustManager(): X509TrustManager? {
    val algorithm = TrustManagerFactory.getDefaultAlgorithm()
    val factory = TrustManagerFactory.getInstance(algorithm)
    factory.init(null as KeyStore?)
    return factory.trustManagers.findInstance()
}

/** A SocketFactory that forces TLS v1.2. */
@RequiresApi(JELLY_BEAN)
private class Tls12SocketFactory(private val delegate: SSLSocketFactory) : SSLSocketFactory() {

    override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites

    override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites

    @Throws(IOException::class)
    override fun createSocket(socket: Socket, host: String, port: Int, autoClose: Boolean): Socket {
        return wrap(delegate.createSocket(socket, host, port, autoClose))
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(host: String, port: Int): Socket {
        return wrap(delegate.createSocket(host, port))
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket {
        return wrap(delegate.createSocket(host, port, localHost, localPort))
    }

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress, port: Int): Socket {
        return wrap(delegate.createSocket(host, port))
    }

    @Throws(IOException::class)
    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket {
        return wrap(delegate.createSocket(address, port, localAddress, localPort))
    }

    private fun wrap(socket: Socket): Socket {
        if (socket is SSLSocket) {
            socket.enabledProtocols = TLS_1_2_ONLY
        }
        return socket
    }
}
