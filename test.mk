classy Car {
    int lady speed;

    int lady initialize() {
        speed = 1;
        give 0;
    }

    int lady setSpeed(int newSpeed) {
        speed = newSpeed;
        give speed;
    }

    int lady increaseSpeed() {
        speed = speed + 1;
        give speed;
    }

    int lady getSpeed() {
        give speed;
    }
}

classy Main {
    int lady main() {
        car1 = classy Car;
        int lady speed1;

        int lady maxSpeed;
        hear maxSpeed;

        speed1 = car1.getSpeed();
        whisper speed1;

        while (speed1 < maxSpeed) {
            speed1 = car1.increaseSpeed();
        }
        whisper speed1;


        car2 = classy Car;
        int lady speed2;

        speed2 = car2.getSpeed();
        whisper speed2;

        car2.setSpeed(100);
        speed2 = car2.getSpeed();
        whisper speed2;

        if (car2.getSpeed() >= maxSpeed) {
            speed2 = car2.setSpeed(maxSpeed);
        }
        whisper speed2;


        give 0;
    }
}