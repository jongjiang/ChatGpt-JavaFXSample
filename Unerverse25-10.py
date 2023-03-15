import random
import matplotlib.pyplot as plt

class Mouse:
    def __init__(self, social_rank):
        self.social_rank = social_rank
        self.age = 0
        self.is_pregnant = False
        self.time_until_birth = 0
        
    def age_mouse(self):
        self.age += 1
    
    def mate(self, partner):
        if self.is_pregnant or partner.is_pregnant:
            return
        
        if self.age < 4 or partner.age < 4:
            return
        
        if abs(self.social_rank - partner.social_rank) > 25:
            return
        
        if random.random() < 0.5:
            return
        
        self.is_pregnant = True
        partner.is_pregnant = True
        self.time_until_birth = 20
    
    def give_birth(self):
        if not self.is_pregnant:
            return
        
        newborn_social_rank = random.randint(1, 100)
        newborn = Mouse(newborn_social_rank)
        self.is_pregnant = False
        self.time_until_birth = 0
        
        return newborn
    
    def die(self):
        death_rate = 0.25
        age_death_rate = 0.15
        age_threshold = 40
        
        if self.age < age_threshold:
            death_rate *= self.age/age_threshold
        else:
            death_rate *= age_death_rate
        
        return random.random() < death_rate
    
    def get_mating_partner(self, mouse_list):
        suitable_partners = [m for m in mouse_list if not m.is_pregnant and abs(self.social_rank - m.social_rank) <= 25 and m.age >= 4]
        if suitable_partners:
            return random.choice(suitable_partners)

class Universe25:
    def __init__(self, initial_population, max_population):
        self.mouse_list = []
        self.max_population = max_population
        for i in range(initial_population):
            social_rank = random.randint(1, 100)
            self.add_mouse(social_rank)
        self.time_step = 0
    
    def add_mouse(self, social_rank):
        if len(self.mouse_list) < self.max_population:
            mouse = Mouse(social_rank)
            self.mouse_list.append(mouse)
    
    def update_population(self):
        self.time_step += 1
        
        for mouse in self.mouse_list:
            mouse.age_mouse()
            if mouse.die():
                self.mouse_list.remove(mouse)
            else:
                mating_partner = mouse.get_mating_partner([m for m in self.mouse_list if m != mouse])
                if mating_partner:
                    mouse.mate(mating_partner)
                
                if mouse.is_pregnant:
                    mouse.time_until_birth -= 1
                    if mouse.time_until_birth == 0:
                        newborn = mouse.give_birth()
                        if newborn:
                            self.mouse_list.append(newborn)
    
    def get_population_size(self):
        return len(self.mouse_list)

initial_population = 50
max_population = 200

universe = Universe25(initial_population, max_population)

populations = []
for i in range(100):
    universe.update_population()
    populations.append(universe.get_population_size())

plt.plot(populations)
plt.xlabel('Time Steps')
plt.ylabel('Population Size')
plt.show()
