import re
from flask import Flask, request
import sys


app = Flask(__name__)

from langchain.llms import OpenAI
from langchain import PromptTemplate, LLMChain
from langchain.chat_models import ChatOpenAI

temperature = 0
@app.route("/gherkin", methods=['post'])
def generateGherkin():
    print("Generating   Gherkin");

    api_key = sys.argv[1]
    if('inputs' not in request.form):
        return "Error: Inputs are required"
    if('description' not in request.form):
        return "Error: Description is required"
    output = ""
    if('output' in request.form):
        output = request.form['output']
    

    # description to Gherkin syntax

    # llm = OpenAI(openai_api_key=api_key,
    #              temperature=temperature,
    #              model='text-davinci-003')
    

    llm = ChatOpenAI(
        model_name='gpt-4',
        temperature=temperature,
        openai_api_key=api_key)
    
    f = open("gherkin.prompt", "r")
    template = f.read()

    prompt = PromptTemplate(template=template, input_variables=["description"])
    llm_chain = LLMChain(prompt=prompt, llm=llm)

    prompt_input = {'description':request.form['description']}
    result = llm_chain.run(prompt_input)

    #Check gerkin
    # llm = OpenAI(openai_api_key=api_key,
    #              temperature=temperature,
    #              model='text-davinci-003')
    
    f = open("check_gherkin.prompt", "r")
    template = f.read()

    prompt = PromptTemplate(template=template, input_variables=["inputs","description","output","gherkin"])  
    llm_chain = LLMChain(prompt=prompt, llm=llm)
    prompt_input = {'output':output, 'inputs':request.form['inputs'], 'description':request.form['description'],'gherkin':result}
    result1 = llm_chain.run(prompt_input)

    # resutls
    cleaned_gherkin = extract_code_and_remove_specific_lines(
                    result1,
                    "GHERKIN:\n",
                    "```"
                )
    if(cleaned_gherkin.count('- YES')):
        cleaned_gherkin = extract_code_and_remove_specific_lines(
                    result,
                    "GHERKIN:\n",
                    "```"
                )
        
    # print(cleaned_gherkin)
    return cleaned_gherkin

def extract_code_and_remove_specific_lines(text, start_marker, line_marker):
    # Extract the code between the start and end markers
    start_idx = text.find(start_marker) + len(start_marker)
    code_block = text[start_idx:].strip()

    # Remove lines containing the specific line marker
    cleaned_code = "\n".join([line for line in code_block.split('\n') if not line.startswith(line_marker)])
    return cleaned_code

@app.route("/groovy", methods=['post'])
def generateGroovy():
    # if('openai-api-key' not in request.headers):
    #     return "Error: OpenAI API key"
    
    
    if('inputs' not in request.form):
        return "Error: Inputs are required"
    
    if('description' not in request.form):
        return "Error: Description is required"
    
    output = ""
    if('output' in request.form):
        output = request.form['output']

    # api_key = request.headers['openai-api-key']

    api_key = sys.argv[1]

    llm = ChatOpenAI(
        model_name='gpt-4',
        temperature=temperature,
        openai_api_key=api_key
        )
    
    f = open("groovy.prompt", "r")
    template = f.read()
    print(request.form['inputs'])
    
    prompt = PromptTemplate(template=template, input_variables=["inputs","description","output"])
    llm_chain = LLMChain(prompt=prompt, llm=llm)
    prompt_input = {'output':output, 'inputs':request.form['inputs'], 'description':request.form['description']}
    result = llm_chain.run(prompt_input)


    # llm = OpenAI(
    #     openai_api_key=api_key,
    #     temperature=temperature,
    #     model='text-davinci-003'
    #     )

    f = open("check_groovy.prompt", "r")
    template = f.read()

    prompt = PromptTemplate(template=template, input_variables=["inputs","description","output","groovy"])
    llm_chain = LLMChain(prompt=prompt, llm=llm)
    prompt_input = {'output':output, 'inputs':request.form['inputs'], 'description':request.form['description'], 'groovy':result}
    result2 = llm_chain.run(prompt_input)

    cleaned_code = extract_code_and_remove_specific_lines(
                    result2,
                    "GROOVY:\n",
                    "```"
                )
    return cleaned_code
    # print(result,"################################",result2)
    # if("NO" in result2):
    #    return "CANNOT CONVERT GHERKIN TO CODE, TRY AGAIN"
    # return result


@app.route("/unittest", methods=['post'])
def generateUnittest():
    
    if('script' not in request.form):
        return "Error: Script is required"
    

    api_key = sys.argv[1]

    llm = ChatOpenAI(
        model_name='gpt-4',
        temperature=temperature,
        openai_api_key=api_key
        )
    
    f = open("unittest.prompt", "r")
    template = f.read()
  
    
    prompt = PromptTemplate(template=template, input_variables=["script"])
    llm_chain = LLMChain(prompt=prompt, llm=llm)
    prompt_input = {'script':request.form['script']}
    result = llm_chain.run(prompt_input)
    # print(result)
    cleaned_code = extract_code_and_remove_specific_lines(
                    result,
                    "UNITTEST CODE:\n",
                    "```"
                )
    print(cleaned_code)
    return extract_substring(cleaned_code)

def extract_substring(input_string):
    start_index = input_string.find("import")
    if start_index == -1:
        return ""
    
    end_index = input_string.rfind("}")
    if end_index == -1:
        return ""
    
    return input_string[start_index:end_index+1]

app.run(debug=True,host='0.0.0.0',port=3001)