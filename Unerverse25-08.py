import random

class Mouse:
    def __init__(self, id_num):
        self.id = id_num
        self.is_alive = True
        self.is_pregnant = False
        self.age = 0
        self.num_offspring = 0
        self.social_rank = 0
    
    def __repr__(self):
        return f"Mouse {self.id}"
    
    def age_mouse(self):
        self.age += 1
        if self.age >= 50:
            self.is_alive = False
        if self.is_pregnant:
            self.num_offspring += 1
    
    def get_pregnant(self):
        self.is_pregnant = True
    
    def give_birth(self):
        self.is_pregnant = False
        self.num_offspring += 1
        return Mouse(id_num=random.randint(1, 100000))
    
    def update_social_rank(self):
        self.social_rank = self.age + self.num_offspring
    

class Universe25:
    def __init__(self, initial_population=20, max_population=200):
        self.mouse_list = [Mouse(id_num) for id_num in range(1, initial_population+1)]
        self.max_population = max_population
        self.time_step = 0
    
    def run_experiment(self):
        while len(self.mouse_list) < self.max_population and len(self.mouse_list) > 0:
            self.time_step += 1
            births = []
            deaths = []
            for mouse in self.mouse_list:
                mouse.age_mouse()
                mouse.update_social_rank()
                if mouse.is_pregnant:
                    births.append(mouse.give_birth())
                if not mouse.is_alive:
                    deaths.append(mouse)
            for dead_mouse in deaths:
                self.mouse_list.remove(dead_mouse)
            for new_mouse in births:
                self.mouse_list.append(new_mouse)
                if len(self.mouse_list) >= self.max_population:
                    break
            if len(self.mouse_list) > 0:
                self.check_behavior()
                self.check_social_behavior()
    
    def check_behavior(self):
        num_mice = len(self.mouse_list)
        if num_mice <= 10:
            print(f"At time {self.time_step}, population: {num_mice}, no abnormal behavior observed.")
        elif num_mice <= 30:
            print(f"At time {self.time_step}, population: {num_mice}, mild aggression and sexual activity observed.")
        elif num_mice <= 100:
            print(f"At time {self.time_step}, population: {num_mice}, increased aggression, stereotypic behavior, and high infant mortality observed.")
        else:
            print(f"At time {self.time_step}, population: {num_mice}, population collapse due to overcrowding.")
    
    def check_social_behavior(self):
        social_ranks = [mouse.social_rank for mouse in self.mouse_list]
        mean_social_rank = sum(social_ranks) / len(social_ranks)
        if mean_social_rank <= 20:
            print(f"At time {self.time_step}, population: {len(self.mouse_list)}, low social cohesion observed.")
        elif mean_social_rank <= 50:
            print(f"At time {self.time_step}, population: {len(self.mouse_list)}, moderate social cohesion observed.")
        else:
            print(f"At time {self.time_step}, population: {len(self.mouse_list)}, high social cohesion observed.")

def implement_social_intervention(self, intervention):
    if intervention == "reduce population":
        self.mouse_list = random.sample(self.mouse_list, k=int(len(self.mouse_list)*0.5))
        print(f"At time {self.time_step}, population reduced to {len(self.mouse_list)}.")
    elif intervention == "improve environment":
        for mouse in self.mouse_list:
            mouse.is_pregnant = False
        print(f"At time {self.time_step}, environment improved, no pregnant mice remaining.")

def get_population_size(self):
    return len(self.mouse_list)


#Now we can run the simulation and observe the social behavior of the mice over time. We can also implement social interventions to improve the well-being of the population. For example, we can reduce the population by 50% or improve the environment by preventing mice from becoming pregnant:
universe = Universe25(initial_population=20, max_population=200)
universe.run_experiment()

#implement social intervention to reduce population
universe.implement_social_intervention("reduce population")

#observe population and social behavior after intervention
universe.run_experiment()

#implement social intervention to improve environment
universe.implement_social_intervention("improve environment")

#observe population and social behavior after intervention
universe.run_experiment()
