package me.jezza.ion;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.List;
import java.util.Objects;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.DatagramPacketDecoder;
import io.netty.handler.codec.DatagramPacketEncoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import me.jezza.ion.bus.EventBus;
import me.jezza.ion.utils.Addresses;
import me.jezza.ion.utils.Pair;
import me.jezza.ion.utils.Strings;

/**
 * @author Jezza
 */
public final class Ion {
	private final EventBus bus;
	private final DatagramChannel channel;
	private final EventLoopGroup group;

	private final InetSocketAddress localAddress;
	private final InetSocketAddress broadcast;

	private Ion(EventBus bus, int port) throws IOException {
		this.bus = bus;
		InetAddress normalised = Strings.normalise(bus.identifier);
		broadcast = new InetSocketAddress(normalised, port);

		group = new NioEventLoopGroup();

		final Pair<NetworkInterface, InetAddress> networkPair = Addresses.getIp4Address();
		final NetworkInterface ni = networkPair.first();
		final InetAddress localAddress = networkPair.second();
		this.localAddress = new InetSocketAddress(localAddress, port);
		channel = (DatagramChannel) new Bootstrap()
				.group(group)
				.channelFactory(() -> new NioDatagramChannel(InternetProtocolFamily.IPv4))
//				.localAddress(localAddress, port)
				.option(ChannelOption.SO_REUSEADDR, true)
//				.option(ChannelOption.SO_BROADCAST, true)
				.option(ChannelOption.IP_MULTICAST_IF, ni)
				.handler(new ChannelInitializer<NioDatagramChannel>() {
					@Override
					protected void initChannel(NioDatagramChannel ch) throws Exception {
						final ChannelPipeline p = ch.pipeline();
						// Encoder
						p.addLast(new DatagramPacketEncoder<>(new ObjectEncoder()));

						// Decoder
						p.addLast(new DatagramPacketDecoder(new ObjectDecoder()));
						p.addLast(new BusNotifier(bus));

					}
				})
				.bind(port)
				.syncUninterruptibly()
				.channel();

		channel.joinGroup(broadcast, channel.config().getNetworkInterface()).syncUninterruptibly();
	}

	private static final class ObjectEncoder extends MessageToMessageEncoder<Serializable> {
		@Override
		protected void encode(ChannelHandlerContext ctx, Serializable msg, List<Object> out) throws Exception {
			ByteBuf buf = ctx.alloc().ioBuffer();
			try (ByteBufOutputStream data = new ByteBufOutputStream(buf);
				 ObjectEncoderOutputStream output = new ObjectEncoderOutputStream(data)) {
				output.writeObject(msg);
				output.flush();
			}
			out.add(buf);
		}
	}

	private static final class ObjectDecoder extends MessageToMessageDecoder<ByteBuf> {
		@Override
		protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
			try (ByteBufInputStream data = new ByteBufInputStream(msg);
				 ObjectDecoderInputStream output = new ObjectDecoderInputStream(data)) {
				out.add(output.readObject());
			}
		}
	}

	private static final class BusNotifier extends SimpleChannelInboundHandler<Serializable> {
		private final EventBus bus;

		BusNotifier(EventBus bus) {
			this.bus = Objects.requireNonNull(bus);
		}

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, Serializable msg) throws Exception {
			bus.post(msg);
		}
	}

	public ChannelFuture post(Serializable event) throws IOException {
		return channel.writeAndFlush(new DefaultAddressedEnvelope<>(event, broadcast, localAddress));
	}

	public EventBus local() {
		return bus;
	}

	public void shutdown() {
		group.shutdownGracefully().syncUninterruptibly();
		channel.close().syncUninterruptibly();
	}

	@Override
	public String toString() {
		return "[Ion:" + bus.identifier + ']';
	}

	public static Ion cluster(String name, int port) {
		try {
			return new Ion(new EventBus(name), port);
		} catch (IOException e) {
			// @TODO Jezza - 23 Aug 2017: Fix this...
			throw new IllegalStateException(e);
		}
	}
}
