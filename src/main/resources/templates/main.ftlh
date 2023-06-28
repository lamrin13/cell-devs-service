#include "../cadmium_v2/include/cadmium/core/logger/csv.hpp"
#include "../cadmium_v2/include/cadmium/core/simulation/root_coordinator.hpp"
#include <limits>
#include "${name}.hpp"

int main(int argc, char *argv[]) {
    // First, we parse the arguments
    if (argc < 3) {
        std::cerr << "ERROR: not enough arguments" << std::endl;
        return -1;
    }

    // Then, we create the model and start the simulation
    auto model = std::make_shared<${name}>("${name}", argv[1]);
    auto rootCoordinator = cadmium::RootCoordinator(model);
    auto logger = std::make_shared<cadmium::CSVLogger>("messages.csv", ";");
    rootCoordinator.setLogger(logger);
    rootCoordinator.start();
    rootCoordinator.simulate((long)std::stoi(argv[2]));
    rootCoordinator.stop();
    return 0;
}
