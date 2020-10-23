package io.grpg.dice;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpg.dice.ThrowDiceServiceGrpc.ThrowDiceServiceImplBase;
import io.grpc.protobuf.services.ProtoReflectionService;

public class ThrowDiceServer {

    private static final Logger logger = Logger.getLogger(ThrowDiceServer.class.getName());
    private final int port;
    private final Server server;

    public ThrowDiceServer(int port) throws IOException {
        this(ServerBuilder.forPort(port), port);
    }

    public ThrowDiceServer(ServerBuilder<?> serverBuilder, int port) {
        this.port = port;
        this.server = serverBuilder.addService(new ThrowDiceService()).addService(ProtoReflectionService.newInstance()).build();
    }

    /** Start serving requests. */
    public void start() throws IOException {
        server.start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown
                // hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    ThrowDiceServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }
        });
    }

    /** Stop serving requests and shutdown resources. */
    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon
     * threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Main method. This comment makes the linter happy.
     */
    public static void main(String[] args) throws Exception {
        ThrowDiceServer server = new ThrowDiceServer(8980);
        server.start();
        server.blockUntilShutdown();
    }

    private static class ThrowDiceService extends ThrowDiceServiceImplBase {

        @Override
        public void throwDice(AskValue request, StreamObserver<GiveValue> responseObserver) {
            responseObserver.onNext(getDiceValue(request));
            responseObserver.onCompleted();
        }

        private GiveValue getDiceValue(AskValue askValue) {

            Random rand = new Random();

            if (askValue.getAsk().equals("Throw dice")) {
                logger.log(Level.INFO, "Getting dice value");
                return GiveValue.newBuilder().setValue(rand.nextInt(Constants.DICE_SIDES)).build();
            } else {
                logger.log(Level.WARNING, "You should write 'Throw dice'");
                return GiveValue.newBuilder().setValue(Constants.DICE_ERROR).build();
            }

        }

    }

}
