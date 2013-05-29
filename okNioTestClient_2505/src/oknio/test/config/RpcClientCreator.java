package oknio.test.config;

import java.io.IOException;

import one.nio.rpc.AbstractRpcClient;

public interface RpcClientCreator {
	public AbstractRpcClient<?> createClient(Configuration conf) throws IOException;
}
