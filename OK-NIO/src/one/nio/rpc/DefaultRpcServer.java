package one.nio.rpc;

import one.nio.net.ConnectionString;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class DefaultRpcServer extends RpcServer<RemoteMethodCall, Object> {
    protected final Map<Long, Method> idToMethod;
    protected final Map<String, Method> id2ToMethod;
    protected final Object instance;

    public DefaultRpcServer(ConnectionString conn, Class<?> serviceClass) throws IOException {
        super(conn);
        this.idToMethod = new ConcurrentHashMap<Long, Method>();
        this.id2ToMethod = new ConcurrentHashMap<String, Method>();
        this.instance = null;

        registerRemoteMethods(serviceClass);
        registerUtilityMethods();
    }

    public DefaultRpcServer(ConnectionString conn, Object serviceInstance) throws IOException {
        super(conn);
        this.idToMethod = new ConcurrentHashMap<Long, Method>();
        this.id2ToMethod = new ConcurrentHashMap<String, Method>();
        this.instance = serviceInstance;

        for (Class<?> cls = serviceInstance.getClass(); cls != Object.class; cls = cls.getSuperclass()) {
            registerRemoteMethods(cls);
            for (Class<?> intf : cls.getInterfaces()) {
                registerRemoteMethods(intf);
            }
        }
        registerUtilityMethods();
    }

    public void registerRemoteMethods(Class cls) {
        for (Method m : cls.getMethods()) {
            if (m.getAnnotation(RemoteMethod.class) != null) {
                registerRemoteMethod(m);
            }
        }
    }

    public void registerRemoteMethod(Method m) {
        idToMethod.put(RemoteMethodCall.calculateMethodId(m), m);
        id2ToMethod.put(m.getName(), m);
    }

    private void registerUtilityMethods() {
        registerRemoteMethod(AbstractRpcClient.provideSerializerMethod);
        registerRemoteMethod(AbstractRpcClient.requestSerializerMethod);
    }

    @Override
    public Object invoke(RemoteMethodCall request) throws Exception {
        Method m = idToMethod.get(request.getMethodId());
        //System.err.println ("DRS.invoke: meth = " + m);
        if (m == null) {
        	//TODO: debug output, delete later
        	String keys = "";
        	for (Iterator<Long> it = idToMethod.keySet().iterator(); it.hasNext(); )
        		keys += (Long.toHexString(it.next())) + "::";
        	
        	String names = "";
        	for (Iterator<String> it = id2ToMethod.keySet().iterator(); it.hasNext(); )
        		names += it.next() + "::";
        	
        	System.err.println (keys);
        	System.err.println (names);
        	//end of dbg output
        	
            throw new NoSuchMethodException("Method id not found: " + Long.toHexString(request.getMethodId()));
        }

        Object o = m.invoke(instance, request.getArgs());
        //System.err.println (o.toString());
        return o;
    }
}
