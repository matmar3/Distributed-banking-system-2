BOX_IMAGE = "hashicorp/bionic64"
BOX_VER = "1.0.282"
NODE_COUNT = 3

Vagrant.configure(2) do |config|

    # ----------------- BankServer -------------------------

    (1..NODE_COUNT).each do |i|
        config.vm.define "bank-server-#{i}" do |subconfig|
            subconfig.vm.synced_folder "./", "/vagrant"
            subconfig.vm.box = BOX_IMAGE
            subconfig.vm.box_version = BOX_VER
            subconfig.vm.hostname = "bank-server-#{i}"
            subconfig.vm.network :private_network, ip: "10.0.1.#{i + 11}"
            subconfig.vm.provision "shell", inline: <<-SHELL
                sudo apt-get update
                sudo apt-get -y install maven
                sudo apt-get -y autoremove
            SHELL
            subconfig.trigger.after :up do |trigger|
                trigger.name = "Run bank-server"
                trigger.run_remote = {
                    inline: <<-SHELL
                        cd /vagrant/bankserver
                        mvn package
                        java -jar target/bank-server.jar -n #{i - 1} &
                    SHELL
                }
            end
        end
    end

end