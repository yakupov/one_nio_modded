package oknio.test.config;

import java.io.IOException;

import one.nio.rpc.AbstractRpcClient;
import one.nio.rpc.ByteArrayRpcClient;
import one.nio.rpc.ByteBufferRpcClient;

public enum ClientType {
	BYTE_BUF_CLIENT(new RpcClientCreator() {
		@Override
		public AbstractRpcClient<?> createClient(Configuration conf) throws IOException {
			return new ByteBufferRpcClient(conf.getConnString(), conf.getBufferSize(), conf.getMaxPoolSize());
		}
	}),
	
	BYTE_ARR_CLIENT(new RpcClientCreator() {
		@Override
		public AbstractRpcClient<?> createClient(Configuration conf) throws IOException {
			return new ByteArrayRpcClient(conf.getConnString());
		}
	});

	
	private RpcClientCreator creator;
	
	private ClientType(RpcClientCreator creator) {
		this.creator = creator;
	}
	
	public RpcClientCreator getCreator() {
		return creator;
	}
}
