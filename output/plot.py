# Import the necessary modules
import matplotlib.pyplot as plt
import pandas as pd
from PIL import Image

path = '/Users/ponce/git/Dat3M/output/csv/'

mapping_method = dict([
    ('assume', 'Dartagnan'),
    ('refinement', 'Refinement')
])

mapping_title = dict([
    ('TSO', 'X86'),
    ('Power', 'PPC'),
    ('ARM8', 'AARCH64'),
    ('Linux', 'LKMM')
])

mapping_files = dict([
    ('two-TSO', pd.read_csv(path + 'X86Test-two.csv')),
    ('refinement-TSO', pd.read_csv(path + 'X86Test-refinement.csv')),
    ('two-Power', pd.read_csv(path + 'PPCTest-two.csv')),
    ('refinement-Power', pd.read_csv(path + 'PPCTest-refinement.csv')),
    ('two-ARM8', pd.read_csv(path + 'AARCH64Test-two.csv')),
    ('refinement-ARM8', pd.read_csv(path + 'AARCH64Test-refinement.csv')),
    ('two-Linux', pd.read_csv(path + 'LinuxTest-two.csv')),
    ('refinement-Linux', pd.read_csv(path + 'LinuxTest-refinement.csv'))
])

##################################################
### Generates bar char for the lock benchmarks ###
##################################################

methods = ["assume", "refinement"]
arch = ["TSO", "Power", "ARM8"]

genmc = pd.read_csv(path + 'genMCTest-genMC.csv')
genmc_df = pd.DataFrame(genmc)

##################################################
### All three together ###
##################################################

df = df_empty = pd.DataFrame({'benchmark' : []})
df['benchmark'] = genmc.iloc[:, 0].apply(lambda x: x.replace(".c", ""))
## colums are: benchmark, result, time
df['GenMC'] = genmc.iloc[:, 2]

for a in arch:
    for m in methods:
        current_df = pd.DataFrame(pd.read_csv(path + "CLocksTest-" + m + "-" + a + ".csv"))
        ## colums are: benchmark, result, time
        df[mapping_method[m]] = current_df.iloc[:, 2]

    plt.figure()
    df.set_index('benchmark').sort_index().plot.bar(log=True, width=0.8)
    plt.title(mapping_title[a])
    plt.xticks(rotation=45, ha="right")
    plt.xlabel("")
    plt.ylabel("Time (ms)")
    plt.tight_layout()
    plt.legend(loc='upper left', bbox_to_anchor=(0.01, 1.025))
    plt.savefig("Figures/" + a + ".png")
    plt.close()

##################################################
### Refinement vs GenMC ###
##################################################

df = df_empty = pd.DataFrame({'benchmark' : []})
df['benchmark'] = genmc.iloc[:, 0].apply(lambda x: x.replace(".c", ""))
## colums are: benchmark, result, time
df['GenMC'] = genmc.iloc[:, 2]

for a in arch:
    current_df = pd.DataFrame(pd.read_csv(path + "CLocksTest-refinement-" + a + ".csv"))
    ## colums are: benchmark, result, time
    df[mapping_method["refinement"]] = current_df.iloc[:, 2]

    my_colors = ['tab:green', 'tab:blue']

    plt.figure()
    df.set_index('benchmark').sort_index().plot.bar(log=True, width=0.8, color=my_colors)
    plt.title(mapping_title[a])
    plt.xticks(rotation=45, ha="right")
    plt.xlabel("")
    plt.ylabel("Time (ms)")
    plt.tight_layout()
    plt.legend(loc='upper left', bbox_to_anchor=(0.01, 1.025))
    plt.savefig("Figures/refinement-vs-genmc-" + a + ".png")
    plt.close()

##################################################
### Refinement vs Dartagnan ###
##################################################

df = df_empty = pd.DataFrame({'benchmark' : []})
df['benchmark'] = genmc.iloc[:, 0].apply(lambda x: x.replace(".c", ""))

for a in arch:
    for m in methods:
        current_df = pd.DataFrame(pd.read_csv(path + "CLocksTest-" + m + "-" + a + ".csv"))
        ## colums are: benchmark, result, time
        df[mapping_method[m]] = current_df.iloc[:, 2]

    my_colors = ['tab:orange', 'tab:blue']

    plt.figure()
    df.set_index('benchmark').sort_index().plot.bar(log=True, width=0.8, color=my_colors)
    plt.title(mapping_title[a])
    plt.xticks(rotation=45, ha="right")
    plt.xlabel("")
    plt.ylabel("Time (ms)")
    plt.tight_layout()
    plt.legend(loc='upper left', bbox_to_anchor=(0.01, 1.025))
    plt.savefig("Figures/refinement-vs-dartagnan-" + a + ".png")
    plt.close()

##################################################
### Generates table for verification result of the lock benchmarks ###
##################################################

df = df_empty = pd.DataFrame({'benchmark' : []})
df['benchmark'] = genmc.iloc[:, 0].apply(lambda x: x.replace(".c", ""))
## colums are: benchmark, result, time
df['GenMC'] = genmc.iloc[:, 1]

for a in arch:
    for m in methods:
        current_df = pd.DataFrame(pd.read_csv(path + "CLocksTest-" + m + "-" + a + ".csv"))
        ## colums are: benchmark, result, time
        df[mapping_method[m]] = current_df.iloc[:, 1]

    colors = []
    for row in df.index:
        if (df.loc[row]['GenMC'] != " N/A") & (df.loc[row]['GenMC'] != df.loc[row]['Dartagnan']):
            if (df.loc[row]['GenMC'] == " FAIL") & (df.loc[row]['Dartagnan'] == " PASS"):
                color = 'tab:green'
            else:
                color = 'tab:red'
        else:
            color = "w"
        colors.append(["w", color, color, color])

    plt.figure()
    plt.title(mapping_title[a])

    the_table = plt.table(cellText=df.values,
        colLabels=df.columns,
        cellColours=colors,
        loc="center")

    ax = plt.gca()
    ax.get_xaxis().set_visible(False)
    ax.get_yaxis().set_visible(False)
    plt.box(on=None)

    plt.savefig("Figures/table-" + a + ".png", bbox_inches='tight', dpi=200)
    plt.close()

##################################################
### Generates plots for the litmus tests ###
##################################################

arch = ["TSO", "Power", "ARM8", "Linux"]

for a in arch:
    plt.figure()
    f, ax = plt.subplots(figsize=(6, 6))
    ax.plot([0, 1], [0, 1], color='r', transform=ax.transAxes)
    plt.title(mapping_title[a])
    plt.xlabel("Dartagnan time (ms)")
    limit = 1000
    if a == "TSO":
        limit = 60
    if a == "Power":
        limit = 120
    if a == "ARM8":
        limit = 90
    if a == "Linux":
        limit = 1000
    plt.xlim([0, limit])
    plt.ylim([0, limit])
    plt.ylabel("Refinement time (ms)")
    plt.plot(
        mapping_files["two-" + a].iloc[:, 2],
        mapping_files["refinement-" + a].iloc[:, 2],
        's')
    plt.savefig("Figures/litmus-" + a + ".png")
    plt.close()