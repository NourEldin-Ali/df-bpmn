from flask import Flask, request
import sys


app = Flask(__name__)

from langchain.llms import OpenAI
from langchain import PromptTemplate, LLMChain
from langchain.chat_models import ChatOpenAI

temperature = 0
@app.route("/gherkin", methods=['post'])
def generateGherkin():
    # if('openai-api-key' not in request.headers):
    #     return "Error: OpenAI API key"

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
        model_name='gpt-3.5-turbo',
        temperature=temperature,
        openai_api_key=api_key)
    
    f = open("gherkin.prompt", "r")
    template = f.read()

    prompt = PromptTemplate(template=template, input_variables=["description"])
    llm_chain = LLMChain(prompt=prompt, llm=llm)

    prompt_input = {'description':request.form['description']}
    result = llm_chain.run(prompt_input)

    #Check gerkin
    llm = OpenAI(openai_api_key=api_key,
                 temperature=temperature,
                 model='text-davinci-003')
    
    f = open("check_gherkin.prompt", "r")
    template = f.read()

    prompt = PromptTemplate(template=template, input_variables=["inputs","description","output"])  
    llm_chain = LLMChain(prompt=prompt, llm=llm)
    prompt_input = {'output':output, 'inputs':request.form['inputs'], 'description':result}
    result1 = llm_chain.run(prompt_input)

    # resutls
    print([result1,"###",result])
    if("NO" in result1):
        return "INVALID REQUEST, CHECK IF YOUR DATA INPUTS AND DESCRIPTION ARE COHERENT, AND TRY AGAIN"
    # return [result1,"###",result]
    return result


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
        model_name='gpt-3.5-turbo',
        temperature=temperature,
        openai_api_key=api_key
        )
    
    f = open("groovy.prompt", "r")
    template = f.read()

    prompt = PromptTemplate(template=template, input_variables=["inputs","description","output"])
    llm_chain = LLMChain(prompt=prompt, llm=llm)
    prompt_input = {'output':output, 'inputs':request.form['inputs'], 'description':request.form['description']}
    result = llm_chain.run(prompt_input)


    llm = OpenAI(
        openai_api_key=api_key,
        temperature=temperature,
        model='text-davinci-003'
        )

    f = open("check_groovy.prompt", "r")
    template = f.read()

    prompt = PromptTemplate(template=template, input_variables=["inputs","description","output"])
    llm_chain = LLMChain(prompt=prompt, llm=llm)
    prompt_input = {'output':output, 'inputs':request.form['inputs'], 'description':request.form['description']}
    result2 = llm_chain.run(prompt_input)

    print(result,"################################",result2)
    return result

app.run(debug=True,host='0.0.0.0',port=3001)