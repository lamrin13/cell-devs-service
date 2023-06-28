int main(int argc, char *argv[]) {
    // First, we parse the arguments
    if (argc < 3) {
        std::cerr << "ERROR: not enough arguments" << std::endl;
        return -1;
    }

    // Then, we create the model and start the simulation
    auto model = std::make_shared<GPT>("HelloWorldservice", jobPeriod, processingTime, obsTime);
    auto rootCoordinator = cadmium::RootCoordinator(model);
    auto logger = std::make_shared<cadmium::CSVLogger>("messages.csv", ";");
    rootCoordinator.setLogger(logger);
    rootCoordinator.start();
    rootCoordinator.simulate(std::numeric_limits<double>::infinity());
    rootCoordinator.stop();
    return 0;
}
